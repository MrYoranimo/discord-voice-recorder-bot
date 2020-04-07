#!/usr/bin/env python3
import struct

ogg_page_header_fmt = "<4sbbQLLLB"
ogg_page_header_fmt_size = struct.calcsize(ogg_page_header_fmt)


class OggPage:
    segments = []

    def __init__(self, data):
        if data[0] != b'OggS':
            raise ValueError("First element in data should be Ogg capture pattern 'OggS', was '{}' instead".format(data[0]))
        self.capture_pattern = data[0].decode()

        if data[1] != 0:
            raise ValueError('OggS version header should be 0, was {} instead'.format(data[1]))
        self.version = data[1]

        self.continuation = data[2] & 0x1 == 0x1
        self.bos = data[2] & 0x2 == 0x2
        self.eos = data[2] & 0x4 == 0x4

        self.granule_position = data[3]
        self.bitstream_serial_number = data[4]
        self.sequence_number = data[5]
        self.checksum = data[6]
        self.n_segments = data[7]

        pass

    def __str__(self):
        return "OggPage(capture_pattern='{}', version='{}', continuation={}, bos={}, eos={}, granule_position={}, " \
               "bitstream_serial_number={:08x}, sequence_number={:08x}, checksum={:08x}, n_segments={})".format(
                    self.capture_pattern,
                    self.version,
                    self.continuation,
                    self.bos,
                    self.eos,
                    self.granule_position,
                    self.bitstream_serial_number,
                    self.sequence_number,
                    self.checksum,
                    self.n_segments,
                )


def main():
    with open('161472952154587136.ogg', 'rb') as file:
        data = file.read()

        while True:
            # break if no data left
            if len(data) == 0:
                break

            page = OggPage(struct.unpack_from(ogg_page_header_fmt, data))

            data = data[ogg_page_header_fmt_size:]
            segment_table = list(data[:page.n_segments])
            data = data[page.n_segments:]
            packets = list()
            last_packet = None

            for segment_length in segment_table:
                if segment_length == 255:
                    packet_finished = False
                else:
                    packet_finished = True

                if last_packet is None:
                    last_packet = data[0:segment_length]
                else:
                    last_packet += data[0:segment_length]

                data = data[segment_length:]

                if packet_finished:
                    packets.append(last_packet)

            print(page, len(packets))

    pass


if __name__ == '__main__':
    main()
