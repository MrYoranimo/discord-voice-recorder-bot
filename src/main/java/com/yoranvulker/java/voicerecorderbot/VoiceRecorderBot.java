package com.yoranvulker.java.voicerecorderbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class VoiceRecorderBot implements EventListener {
    @Override
    public void onEvent(@Nonnull GenericEvent genericEvent) {
        if (genericEvent instanceof ReadyEvent) {
            System.out.println("VoiceRecorderBot has been activated.");
        }

        if (genericEvent instanceof GuildVoiceUpdateEvent) {
            GuildVoiceUpdateEvent event = (GuildVoiceUpdateEvent) genericEvent;

            if (event.getChannelJoined() != null) {
                VoiceChannel joinedChannel = event.getChannelJoined();
                Guild joinedGuild = joinedChannel.getGuild();
                Random dice = new Random();
                int diceroll = dice.nextInt(3);
                if (diceroll == 0) joinedGuild.getAudioManager().openAudioConnection(joinedChannel);
            }

            if (event.getChannelLeft() != null) {
                VoiceChannel leftChannel = event.getChannelLeft();
                Guild leftGuild = leftChannel.getGuild();

                if (leftGuild.getAudioManager().getConnectedChannel() == leftChannel) {
                    // determine if normal users are left
                    List<Member> channelMembers = leftChannel.getMembers().stream()
                            .filter(member -> !member.getUser().isBot())
                            .collect(Collectors.toList());

                    if (channelMembers.size() == 0) {
                        leftGuild.getAudioManager().closeAudioConnection();
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            // create instance
            VoiceRecorderBot instance = new VoiceRecorderBot();

            // make connection to Discord API
            JDABuilder builder = new JDABuilder(args[0])
                    .setActivity(Activity.listening("you 👂"))
                    .addEventListeners(instance)
                    .addEventListeners(new MessageEventListener());
            JDA jda = builder.build();

            // wait until JDA has been initialized
            jda.awaitReady();
            System.out.printf("Connected to the following servers: %s%n", jda.getGuilds());

            for (Guild guild : jda.getGuilds()) {
                for (VoiceChannel channel : guild.getVoiceChannels()) {
                    List<Member> channelMembers = channel.getMembers().stream()
                            .filter(member -> !member.getUser().isBot())
                            .collect(Collectors.toList());

                    if (channelMembers.size() > 0) {
                        guild.getAudioManager().openAudioConnection(channel);
                        guild.getAudioManager().setReceivingHandler(new AivdAudioReceiveHandler(jda));
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}