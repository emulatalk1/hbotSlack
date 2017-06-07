package hieu.hbot.slackbot;

import core.bot.models.Event;
import core.bot.models.Message;
import core.bot.slack.Bot;
import core.bot.slack.EventType;
import core.bot.slack.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.regex.Matcher;

/**
 *
 * @author Spectre
 */
@Component
public class SlackBot extends Bot {

    private static final Logger logger = LoggerFactory.getLogger(SlackBot.class);

    /*
     Slack token từ file "application.properties".
     */
    @Value("${slackBotToken}")
    private String slackToken;

    @Override
    public String getSlackToken() {
        return slackToken;
    }

    @Override
    public Bot getSlackBot() {
        return this;
    }

    /*
     Được gọi khi bot nhận được đề cập trực tiếp (@botname: message) Hoặc một tin nhắn trực tiếp.
     
     @param session
     @param event
     */
    @Controller(events = {EventType.DIRECT_MENTION, EventType.DIRECT_MESSAGE})
    public void onReceiveDM(WebSocketSession session, Event event) {
        reply(session, event, new Message("Xin chào, mình là " + slackService.getCurrentUser().getName()));
    }

    /*
     Được gọi khi bot nhận được một event kiểu MESSAGE với text thỏa mãn kiểu {@code ([a-z ]{2})(\d+)([a-z ]{2})}. 
     Những kiểu như "ab12xy" hay "ab2bc" sẽ gọi tới method này.
    
     @param session
     @param event
     */
    @Controller(events = EventType.MESSAGE, pattern = "^([a-z ])(\\d+)([a-z ])$")
    public void onReceiveMessage(WebSocketSession session, Event event, Matcher matcher) {
        reply(session, event, new Message("Nguyên bản: " + matcher.group(0) + "\n" +
                "Nhóm đầu: " + matcher.group(1) + "\n" +
                "Nhóm giữa: " + matcher.group(2) + "\n" +
                "Nhóm cuối: " + matcher.group(3)));
    }

    /*
     Được gọi khi một item được pin trên channel.
  
     @param session
     @param event
     */
    @Controller(events = EventType.PIN_ADDED)
    public void onPinAdded(WebSocketSession session, Event event) {
        reply(session, event, new Message("Thông tin bạn pin lên thật hữu ích! :grimacing:"));
    }
    
    /*
     Được gọi khi một người mới tham gia channel.
    */
    @Controller(events = EventType.MEMBER_JOINED_CHANNEL)
    public void onChannelJoined(WebSocketSession session, Event event) {
        reply(session, event, new Message("Xin chào :grimacing:"));
    }
    /*
     Được gọi khi bot nhận một event kiểu FILE được chia sẽ.
     
     @param session
     @param event
     */
    @Controller(events = EventType.FILE_SHARED)
    public void onFileShared(WebSocketSession session, Event event) {
        logger.info("File shared: {}", event);
    }


    /*
     Tính năng trò chuyện của bot. Method này bắt đầu cuộc trò chuyện
     
     @param session
     @param event
     */
    @Controller(pattern = "(Hẹn thời gian chơi dota)", next = "confirmTiming")
    public void setupMeeting(WebSocketSession session, Event event) {
        startConversation(event, "confirmTiming");   // bắt đầu trò chuyện
        reply(session, event, new Message("Thời gian (VD: 15:30) mà bạn muốn hẹn là?"));
    }

    /*
     Method này được móc nối với {@link SlackBot#setupMeeting(WebSocketSession, Event)}.
     
     @param session
     @param event
     */
    @Controller(next = "askTimeForMeeting")
    public void confirmTiming(WebSocketSession session, Event event) {
        reply(session, event, new Message("Thời gian hẹn chơi dota là " + event.getText() +
                ". Bạn có muốn hẹn như vậy vào ngày mai không?"));
        nextConversation(event);    // Nhảy tới câu hỏi tiếp theo trong cuộc trò chuyện
    }

    /*
     Method này được móc nối với {@link SlackBot#confirmTiming(WebSocketSession, Event)}.
     
     @param session
     @param event
     */
    @Controller(next = "askWhetherToRepeat")
    public void askTimeForMeeting(WebSocketSession session, Event event) {
        if (event.getText().contains("có")) {
            reply(session, event, new Message("Được rồi! Bạn có muốn tôi nhắc không?"));
            nextConversation(event);    // Nhảy tới câu hỏi tiếp theo trong cuộc trò chuyện  
        } else {
            reply(session, event, new Message("Oke, không vấn đề gì. Bạn luôn có thể lên thời gian bằng lệnh 'Hẹn thời gian chơi dota'."));
            stopConversation(event);    // Chỉ dừng cuộc trò chuyện khi user nói "no"
        }
    }

    /*
     Method này được móc nối với {@link SlackBot#askTimeForMeeting(WebSocketSession, Event)}.
     
     @param session
     @param event
     */
    @Controller
    public void askWhetherToRepeat(WebSocketSession session, Event event) {
        if (event.getText().contains("có")) {
            reply(session, event, new Message("Oke bossss! Mình sẽ nhắc bạn trước lịch hẹn vào ngày mai."));
        } else {
            reply(session, event, new Message("Ghê! Thông minh vãi, tự nhớ, hihi :)"));
        }
        stopConversation(event);    // dừng cuộc trò chuyện.
    }
}