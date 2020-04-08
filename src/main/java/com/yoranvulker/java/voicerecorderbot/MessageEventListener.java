package com.yoranvulker.java.voicerecorderbot;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import javax.annotation.Nonnull;
import java.util.*;

public class MessageEventListener implements EventListener {
    private interface ResponseMessage {
        String render(Message message);
    }

    List<ResponseMessage> responses = new ArrayList<>();
    Map<String, List<ResponseMessage>> conditionalResponses = new HashMap<>();

    public MessageEventListener() {
        conditionalResponses.put("aivd", new ArrayList<>());
        conditionalResponses.put("nsa", new ArrayList<>());
        conditionalResponses.put("fbi", new ArrayList<>());
        conditionalResponses.put("cia", new ArrayList<>());
        conditionalResponses.put("gchq", new ArrayList<>());
        conditionalResponses.put("politie", new ArrayList<>());
        conditionalResponses.put("police", new ArrayList<>());
        conditionalResponses.put("defensie", new ArrayList<>());
        conditionalResponses.put("dod", new ArrayList<>());
        conditionalResponses.put("cicada3301", new ArrayList<>());
        conditionalResponses.put("fox-it", new ArrayList<>());
        conditionalResponses.put("fox it", new ArrayList<>());
        conditionalResponses.put("kgb", new ArrayList<>());
        conditionalResponses.put("ik doe de deur dicht", new ArrayList<>());
        conditionalResponses.put("noord korea", new ArrayList<>());
        conditionalResponses.put("north korea", new ArrayList<>());
        conditionalResponses.put("papa kim", new ArrayList<>());

        ResponseMessage messageEyes = (message) -> ":eyes:";
        conditionalResponses.get("aivd").add(messageEyes);

        ResponseMessage messageImWatchingYou = (message) -> String.format("I am watching you, %s", message.getAuthor().getAsMention());
        conditionalResponses.get("nsa").add(messageImWatchingYou);
        conditionalResponses.get("aivd").add(messageImWatchingYou);
        conditionalResponses.get("gchq").add(messageImWatchingYou);
        responses.add(messageImWatchingYou);

        ResponseMessage messageMindYourDecisions = (message) -> String.format("Mind your decisions, %s", message.getAuthor().getAsMention());
        conditionalResponses.get("cicada3301").add(messageMindYourDecisions);
        responses.add(messageMindYourDecisions);

        ResponseMessage messageIgnoreThat = (message) -> "Let's ignore that for now.";
        responses.add(messageIgnoreThat);

        ResponseMessage messageBetweenYouAndMe = (message) -> "Let's keep that between you and me.";
        conditionalResponses.get("fox-it").add(messageBetweenYouAndMe);
        conditionalResponses.get("fox it").add(messageBetweenYouAndMe);
        responses.add(messageBetweenYouAndMe);

        ResponseMessage messageLetsNotTellAnybody = (message) -> String.format("Let's not tell anybody I was ever here, %s", message.getAuthor().getAsMention());
        responses.add(messageLetsNotTellAnybody);

        ResponseMessage messageYesQuestion = (message) -> "Yes?";
        responses.add(messageYesQuestion);

        ResponseMessage messageNegative = (message) -> "Negative";
        conditionalResponses.get("defensie").add(messageNegative);
        conditionalResponses.get("dod").add(messageNegative);
        responses.add(messageNegative);

        ResponseMessage messageRogerThat = (message) -> "Roger that";
        conditionalResponses.get("defensie").add(messageRogerThat);
        conditionalResponses.get("dod").add(messageRogerThat);
        responses.add(messageRogerThat);

        ResponseMessage messageKgbResponse = (message) -> "Feel united, comrade! https://www.youtube.com/watch?v=U06jlgpMtQs";
        conditionalResponses.get("kgb").add(messageKgbResponse);

        ResponseMessage messageNorthKoreaResponse = (message) -> "Kim says hi!, https://www.youtube.com/watch?v=gGclRydi1NY";
        conditionalResponses.get("noord korea").add(messageNorthKoreaResponse);
        conditionalResponses.get("north korea").add(messageNorthKoreaResponse);
        conditionalResponses.get("papa korea").add(messageNorthKoreaResponse);

        ResponseMessage messagePermissionDenied = (message) -> "Permission denied";
        conditionalResponses.get("fox it").add(messagePermissionDenied);
        conditionalResponses.get("fox-it").add(messagePermissionDenied);
        responses.add(messagePermissionDenied);

        ResponseMessage messageFourEighteen = (message) -> "418 I'm a teapot";
        conditionalResponses.get("fox it").add(messageFourEighteen);
        conditionalResponses.get("fox-it").add(messageFourEighteen);
        responses.add(messageFourEighteen);

        ResponseMessage messageFourOhThree = (message) -> "403 Forbidden";
        conditionalResponses.get("fox it").add(messageFourOhThree);
        conditionalResponses.get("fox-it").add(messageFourOhThree);
        responses.add(messageFourOhThree);

        ResponseMessage messageFalsePositive = (message) -> "False positive, I'm not here";
        responses.add(messageFalsePositive);

        ResponseMessage messageShht = (message) -> "Sssssshhht";
        responses.add(messageShht);

        ResponseMessage messageDontStopMeNow = (message) -> "🎵 Don't stop me now! 🎶";
        conditionalResponses.get("gchq").add(messageDontStopMeNow);
        responses.add(messageDontStopMeNow);

        ResponseMessage messageEagleIsWatchingYou = (message) -> String.format("The Eagle is watching you, %s.", message.getAuthor().getAsMention());
        conditionalResponses.get("cia").add(messageEagleIsWatchingYou);
        responses.add(messageEagleIsWatchingYou);

        ResponseMessage messageOpenUp = (message) -> "AIVD, OPEN UP!";
        conditionalResponses.get("ik doe de deur dicht").add(messageOpenUp);
        conditionalResponses.get("aivd").add(messageOpenUp);
        conditionalResponses.get("fbi").add(messageOpenUp);
        responses.add(messageOpenUp);

        ResponseMessage messageThatsIllegal = (message) -> "Wait, that's illegal!";
        conditionalResponses.get("politie").add(messageThatsIllegal);
        conditionalResponses.get("police").add(messageThatsIllegal);
        responses.add(messageThatsIllegal);
    }

    @Override
    public void onEvent(@Nonnull GenericEvent genericEvent) {
        if (!(genericEvent instanceof MessageReceivedEvent)) {
            return;
        }

        MessageReceivedEvent event = (MessageReceivedEvent) genericEvent;
        Message message = event.getMessage();
        System.out.printf("Message received from '%s': '%s'%n", message.getAuthor().getAsTag(), message);

        if (event.getAuthor() == event.getJDA().getSelfUser()) {
            return;
        }

        boolean respondToMessage = false;
        Random dice = new Random();

        if (message.isMentioned(event.getJDA().getSelfUser())) {
            respondToMessage = true;
        } else {
            for (String word : conditionalResponses.keySet()) {
                // respond to this word if it's in the message sent by the user, stop program flow
                if (message.getContentRaw().toLowerCase().contains(word.toLowerCase())) {
                    // get all possible responses, pick one at random and return that message
                    List<ResponseMessage> responses = conditionalResponses.get(word);
                    ResponseMessage responseMessage = responses.get(dice.nextInt(responses.size()));
                    String responseMessageText = responseMessage.render(message);
                    MessageAction action = event.getChannel().sendMessage(responseMessageText);
                    action.submit();

                    return;
                }
            }

            // if none of the words set for conditional messages is found in the message,
            // respond to 1 in 50 messages.
            int result = dice.nextInt(50);
            System.out.printf("Dice threw %d%n", result);

            // dice threw 0
            if (result == 0) {
                respondToMessage = true;
            }
        }

        if (respondToMessage) {
            System.out.println("Sending response");
            String response = responses.get(dice.nextInt(responses.size())).render(message);
            MessageAction action = event.getChannel().sendMessage(response);
            action.submit();
        }
    }
}
