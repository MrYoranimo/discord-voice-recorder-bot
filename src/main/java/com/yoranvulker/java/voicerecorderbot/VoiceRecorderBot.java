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
    public static class TenMinRun extends Thread {

        private JDA jda;
        public TenMinRun(JDA jda) {
            this.jda = jda;
        }

        public void run() {
            while (true) {
                Random dice = new Random();
                int diceRoll = dice.nextInt(8);
                System.out.println("Thread 10 min run dicerole returns: " + diceRoll);

                for (Guild guild : this.jda.getGuilds()) {
                    for (VoiceChannel channel : guild.getVoiceChannels()) {
                        List<Member> channelMembers = channel.getMembers().stream()
                                .filter(member -> !member.getUser().isBot())
                                .collect(Collectors.toList());

                        if (channelMembers.size() > 0) {
                            if(diceRoll == 5) guild.getAudioManager().openAudioConnection(channel);
                            guild.getAudioManager().setReceivingHandler(new BotAudioReceiveHandler(jda));
                            break;
                        }
                    }
                }
                try {
                    sleep(180000);
                    this.jda.getGuilds().forEach(guild -> guild.getAudioManager().closeAudioConnection());
                    sleep(420000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onEvent(@Nonnull GenericEvent genericEvent) {
        if (genericEvent instanceof ReadyEvent) {
            System.out.println("VoiceRecorderBot has been activated.");
        }

        if (genericEvent instanceof GuildVoiceUpdateEvent) {
            GuildVoiceUpdateEvent event = (GuildVoiceUpdateEvent) genericEvent;

            /* TODO: Implement the code below so that additionally to check every 10 min it also has a 1 in 20 chance
                to join when a user joins a voicechannel, this already works.
                What needs to be implemented is that it has to wait 3 min and then leave.
            */
           /* if (event.getChannelJoined() != null) {
                VoiceChannel joinedChannel = event.getChannelJoined();
                Guild joinedGuild = joinedChannel.getGuild();
                Random dice = new Random();
                int diceRoll = dice.nextInt(20);
                System.out.println("A person joined a voicechannel (1 in 20 chance that the bot joins) dicerole returns: " + diceRoll);
                if (diceRoll == 10) joinedGuild.getAudioManager().openAudioConnection(joinedChannel);
            }*/

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

            new TenMinRun(jda).start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
