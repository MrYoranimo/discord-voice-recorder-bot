package nl.aivd.voicerecorderbot;

import net.dv8tion.jda.api.audio.OpusPacket;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;

public class OggOpusOutputStream implements Closeable, Flushable {
    private static final short[] PAGE_TEMPLATE = new short[] {
            /* page header */
            0x4f, 0x67, 0x67, 0x53, // 'OggS'
            0x00, 0x00, // structure version 0, regular packet
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // granule position
            0x00, 0x00, 0x00, 0x00, // stream serial number
            0x02, 0x00, 0x00, 0x00, // page sequence number
            0x00, 0x00, 0x00, 0x00, // CRC32 checksum (to be calculated)
            0x00, // amount of lacing values
    };

    private static final int PAGE_HEADER_SIZE = PAGE_TEMPLATE.length; // excluding lacing table
    private static final int PAGE_GRANULE_POSITION_OFFSET = 6;
    private static final int PAGE_STREAM_SERIAL_NUMBER_OFFSET = 14;
    private static final int PAGE_SEQUENCE_NUMBER_OFFSET = 18;
    private static final int PAGE_CRC32_CHECKSUM_OFFSET = 22;
    private static final int PAGE_SEGMENT_TABLE_SIZE_OFFSET = 26;

    private OutputStream outputStream;
    private short[] serialNumber;
    private List<byte[]> packets;
    private int[] checksumTable;
    private long packetCount = 0;
    private long pageOffset = 2;
    private long granulePosition = 48000;

    public OggOpusOutputStream(OutputStream outputStream) throws IOException {
        this.outputStream = outputStream;
        this.checksumTable = generateChecksumTable();
        this.serialNumber = generateSerialNumber();
        this.packets = new ArrayList<>();

        writeInitialData();
    }

    private static int[] generateChecksumTable() {
        int[] table = new int[256];
        int polynomial = 0x04c11db7;

        for (int i = 0; i < table.length; i++) {
            int remainder = i << 24;

            for (int j = 0; j < 8; j++) {
                if ((remainder & 0x80000000) != 0) {
                    remainder = (remainder << 1) ^ polynomial;
                } else {
                    remainder = remainder << 1;
                }

                table[i] = remainder;
            }
        }

        return table;
    }

    /**
     * Generates a new short[] of 4 bytes, containing a serial number
     * that can be used to uniquely identify a Ogg stream.
     *
     * @return short[4] filled with random values
     */
    private static short[] generateSerialNumber() {
        short[] serialNumber = new short[4];

        Random random = new Random();
        for (int i = 0; i < serialNumber.length; i++) {
            serialNumber[i] = (short) (random.nextInt() & 0xff);
        }

        return serialNumber;
    }

    /**
     * Converts a short array to its byte array equivalent by masking only the lower 8 bits.
     *
     * @param input short[] to be converted to a byte[]
     * @return byte[] of equal length of input short[], where each indivial short has been masked by 0x00ff
     */
    private byte[] byteArrayFromShorts(short[] input) {
        byte[] output = new byte[input.length];

        for (int i = 0; i < input.length; i++) {
            output[i] = (byte) (input[i] & 0x00ff);
        }

        return output;
    }

    /**
     * Writes the initial pages of the Opus format in Ogg container
     *
     * @throws IOException when the bytes of the pages could not be written to the output stream
     */
    private void writeInitialData() throws IOException {
        short[] opusHeadPage = new short[] {
                /* page header */
                0x4f, 0x67, 0x67, 0x53, // 'OggS'
                0x00, 0x02, // version 0, first packet in stream
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // granule position
                0x00, 0x00, 0x00, 0x00, // stream serial number
                0x00, 0x00, 0x00, 0x00, // page sequence number
                0x00, 0x00, 0x00, 0x00, // CRC32 checksum (to be calculated)
                0x01, // amount of lacing values
                0x13, // 0x13 = 19 bytes of data

                /* page data */
                0x4f, 0x70, 0x75, 0x73, 0x48, 0x65, 0x61, 0x64, // 'OpusHead'
                0x01, 0x02, 0x00, 0x0f, // version 1, 2 channels, 0 pre-skip
                0x80, 0xbb, 0x00, 0x00, // sample rate (48000)
                0x00, 0x00, 0x00, // output gain, mapping family
        };

        populateStreamSerialNumberHeader(opusHeadPage);
        populatePageCrcHeader(opusHeadPage);

        short[] opusTagsPage = new short[] {
                /* page header */
                0x4f, 0x67, 0x67, 0x53, // 'OggS'
                0x00, 0x00, // structure version 0, regular packet
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // granule position
                0x00, 0x00, 0x00, 0x00, // stream serial number
                0x01, 0x00, 0x00, 0x00, // page sequence number
                0x00, 0x00, 0x00, 0x00, // CRC32 checksum (to be calculated)
                0x01, // amount of lacing values
                0x20, // 0x20 = 32 bytes of data

                /* page data */
                0x4f, 0x70, 0x75, 0x73, 0x54, 0x61, 0x67, 0x73, // 'OpusTags'
                0x10, 0x00, 0x00, 0x00, // vendor string length (0)
                0x56, 0x6f, 0x69, 0x63, 0x65, 0x52, 0x65, 0x63, 0x6f, 0x72, 0x64, 0x65, 0x72, 0x42, 0x6f, 0x74,
                0x00, 0x00, 0x00, 0x00, // user string count (0)
        };

        populateStreamSerialNumberHeader(opusTagsPage);
        populatePageCrcHeader(opusTagsPage);

        // write OpusHead and OpusTags pages
        this.outputStream.write(byteArrayFromShorts(opusHeadPage));
        this.outputStream.write(byteArrayFromShorts(opusTagsPage));
        this.outputStream.flush();
    }

    private void populatePageCrcHeader(short[] streamPage) {
        int checksum = 0;

        for (short s : streamPage) {
            int tableIndex = checksum >>> 24 ^ s;
            checksum = checksum << 8 ^ this.checksumTable[tableIndex];
        }

        // little ending, least significant byte comes first
        streamPage[PAGE_CRC32_CHECKSUM_OFFSET    ] = (short) (checksum        & 0xff);
        streamPage[PAGE_CRC32_CHECKSUM_OFFSET + 1] = (short) (checksum >>> 8  & 0xff);
        streamPage[PAGE_CRC32_CHECKSUM_OFFSET + 2] = (short) (checksum >>> 16 & 0xff);
        streamPage[PAGE_CRC32_CHECKSUM_OFFSET + 3] = (short) (checksum >>> 24 & 0xff);
    }

    public synchronized void write(byte[] opusAudio) throws IOException {
        byte[] buffer = opusAudio;

        while (true) {
            if (buffer.length < 255) {
                this.packets.add(buffer);
                break;
            }

            byte[] segment = new byte[255];
            byte[] remaining = new byte[buffer.length - 255];

            // read current segment (byte 0 through 254)
            System.arraycopy(buffer, 0, segment, 0, segment.length);

            // read remaining segments (from byte 255)
            System.arraycopy(buffer, segment.length, remaining, 0, remaining.length);

            this.packets.add(segment);
            buffer = remaining;
        }

        // increase packet count
        packetCount++;

        // per page, maximum of 50 packets (not equal to amount of segments)
        if (packetCount % 50 == 0) {
            this.flush();
        }
    }

    private void populateStreamSerialNumberHeader(short[] page) {
        System.arraycopy(this.serialNumber, 0, page, PAGE_STREAM_SERIAL_NUMBER_OFFSET, this.serialNumber.length);
    }

    @Override
    public void close() throws IOException {
        // flush buffered data before closing the output stream
        this.flush();

        // close the internal output stream
        this.outputStream.close();
    }

    @Override
    public synchronized void flush() throws IOException {
        // calculate space in the page that will be occupied by the segments in the buffer
        int segmentSizesSum = this.packets.stream().mapToInt(segment -> segment.length).sum();
        // TODO Make sure this.segments isn't bigger than 255, because table size will overflow

        // Page contains header, segment table, and segments
        int pageSize = PAGE_HEADER_SIZE + this.packets.size() + segmentSizesSum;
        short[] page = new short[pageSize];

        // copy header template to new page
        System.arraycopy(PAGE_TEMPLATE, 0, page, 0, PAGE_TEMPLATE.length);
        populateStreamSerialNumberHeader(page);

        page[PAGE_SEGMENT_TABLE_SIZE_OFFSET] = (short) (this.packets.size() & 0xff);
        int offset = PAGE_HEADER_SIZE + this.packets.size();

        for (int i = 0; i < this.packets.size(); i++) {
            byte[] currentSegment = this.packets.get(i);

            // add segment size to table
            page[PAGE_HEADER_SIZE + i] = (short) (currentSegment.length & 0xff);

            // add segment to page
            for (int currentByte = 0; currentByte < currentSegment.length; currentByte++) {
                page[offset + currentByte] = (short) ((short) currentSegment[currentByte] & 0xff);
            }

            // update offset to be at first byte after current segment in page
            offset += currentSegment.length;
        }

        // populate required fields in header
        updatePageOffset(page);
        updateGranulatePosition(page);
        populatePageCrcHeader(page);

        // write generated page to output stream
        this.outputStream.write(byteArrayFromShorts(page));

        // flush internal output stream
        this.outputStream.flush();

        // remove segments from buffer
        this.packets.clear();
    }

    private void updateGranulatePosition(short[] page) {
        page[PAGE_GRANULE_POSITION_OFFSET    ] = (short) (this.granulePosition       & 0xff);
        page[PAGE_GRANULE_POSITION_OFFSET + 1] = (short) (this.granulePosition >> 8  & 0xff);
        page[PAGE_GRANULE_POSITION_OFFSET + 2] = (short) (this.granulePosition >> 16 & 0xff);
        page[PAGE_GRANULE_POSITION_OFFSET + 3] = (short) (this.granulePosition >> 24 & 0xff);
        page[PAGE_GRANULE_POSITION_OFFSET + 4] = (short) (this.granulePosition >> 32 & 0xff);
        page[PAGE_GRANULE_POSITION_OFFSET + 5] = (short) (this.granulePosition >> 40 & 0xff);
        page[PAGE_GRANULE_POSITION_OFFSET + 6] = (short) (this.granulePosition >> 48 & 0xff);
        page[PAGE_GRANULE_POSITION_OFFSET + 7] = (short) (this.granulePosition >> 56 & 0xff);

        this.granulePosition += 48000;
    }

    private void updatePageOffset(short[] page) {
        page[PAGE_SEQUENCE_NUMBER_OFFSET    ] = (short) (this.pageOffset       & 0xff);
        page[PAGE_SEQUENCE_NUMBER_OFFSET + 1] = (short) (this.pageOffset >> 8  & 0xff);
        page[PAGE_SEQUENCE_NUMBER_OFFSET + 2] = (short) (this.pageOffset >> 16 & 0xff);
        page[PAGE_SEQUENCE_NUMBER_OFFSET + 3] = (short) (this.pageOffset >> 24 & 0xff);

        this.pageOffset++;
    }
}

