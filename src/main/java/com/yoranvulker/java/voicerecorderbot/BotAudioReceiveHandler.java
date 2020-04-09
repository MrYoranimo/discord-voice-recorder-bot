package com.yoranvulker.java.voicerecorderbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.OpusPacket;
import net.dv8tion.jda.api.entities.User;

import javax.annotation.Nonnull;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class BotAudioReceiveHandler implements AudioReceiveHandler {
    JDA jdaInstance;
    Map<Long, OggOpusOutputStream> streams = new HashMap<>();

    public BotAudioReceiveHandler(JDA jda) {
        this.jdaInstance = jda;
    }

    @Override
    public boolean canReceiveCombined() {
        return false;
    }

    @Override
    public boolean canReceiveEncoded() {
        return true;
    }

    @Override
    public boolean canReceiveUser() {
        return false;
    }

//    @Override
//    public void handleUserAudio(@Nonnull UserAudio userAudio) {
//        long userId = userAudio.getUser().getIdLong();
//        byte[] rawAudio = userAudio.getAudioData(1.0f);
//
//        try {
//            OutputStream outputStream = streams.get(userId);
//
//            if (outputStream == null) {
//                outputStream = new FileOutputStream(String.format("%d.wav", userId));
//                streams.put(userId, outputStream);
//            }
//
//            outputStream.write(rawAudio);
//        } catch (Exception ignored) {}
//    }

    @Override
    public void handleEncodedAudio(@Nonnull OpusPacket packet) {
        long userId = packet.getUserId();
//        System.out.println(Arrays.toString(encodedAudio));

        try {
            OggOpusOutputStream outputStream = streams.get(packet.getUserId());

            if (outputStream == null) {
                User user = this.jdaInstance.getUserById(userId);
                String userAsString = user == null ? "" + userId : String.format("%s#%s", user.getName(), user.getDiscriminator());

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HHmmss");
                Date date = new Date();

                String filename = String.format("%s-%s.ogg", userAsString, format.format(date));

                System.out.printf("Creating file '%s'%n", filename);
                outputStream = new OggOpusOutputStream(new FileOutputStream(filename));
                this.streams.put(packet.getUserId(), outputStream);
            }

            outputStream.write(packet.getOpusAudio());
        } catch (Exception ignored) {}
    }
}
