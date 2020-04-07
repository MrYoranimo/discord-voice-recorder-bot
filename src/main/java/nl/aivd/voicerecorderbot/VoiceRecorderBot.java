package nl.aivd.voicerecorderbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class VoiceRecorderBot implements EventListener {
    @Override
    public void onEvent(@Nonnull GenericEvent event) {
        if (event instanceof ReadyEvent) {
            System.out.println("VoiceRecorderBot has been activated.");
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
