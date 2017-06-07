package hieu.hbot.slackbot;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.bot.models.Attachment;
import core.bot.models.RichMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

/**
 *
 * @author Spectre
 */
@Component
public class SlackWebhooks {

    private static final Logger logger = LoggerFactory.getLogger(SlackWebhooks.class);

    /*
     Get Url trong khi đang configure incoming webhook mới
     trên Slack.
     <a href="https://my.slack.com/services/new/incoming-webhook/"></a>.
     */
    @Value("${slackIncomingWebhookUrl}")
    private String slackIncomingWebhookUrl;

    /*
     Làm một POST call tới incoming webhook url.
     */
    @PostConstruct
    public void invokeSlackWebhook() {
        RestTemplate restTemplate = new RestTemplate();
        RichMessage richMessage = new RichMessage("Just to test Slack's incoming webhooks.");
        // set attachments
        Attachment[] attachments = new Attachment[1];
        attachments[0] = new Attachment();
        attachments[0].setText("Some data relevant to your users.");
        richMessage.setAttachments(attachments);

        try {
            logger.debug("Reply (RichMessage): {}", new ObjectMapper().writeValueAsString(richMessage));
        } catch (JsonProcessingException e) {
            logger.debug("Error parsing RichMessage: ", e);
        }

        try {
            restTemplate.postForEntity(slackIncomingWebhookUrl, richMessage.encodedMessage(), String.class);
        } catch (RestClientException e) {
            logger.error("Error posting to Slack Incoming Webhook: ", e);
        }
    }
}
