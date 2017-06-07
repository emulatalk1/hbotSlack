/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core.bot.slack;

import core.bot.models.Event;
import core.bot.models.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.lang.reflect.Method;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

/**
 *
 * @author Spectre
 */
public abstract class Bot {
    
    private static final Logger logger = LoggerFactory.getLogger(Bot.class);
    
    private final Map<String, List<MethodWrapper>> eventToMethodsMap = new HashMap();
    
    private final Map<String, MethodWrapper> methodNameMap = new HashMap();
    
    private final List<String> conversationMethodNames = new ArrayList();
    
    private final Map<String, Queue<MethodWrapper>> conversationQueueMap = new HashMap();
    
    @Autowired
    protected SlackService slackService;
    
    public abstract String getSlackToken();
    
    public abstract Bot getSlackBot();
    
    public Bot() {
        Method[] methods = this.getClass().getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Controller.class)) {
                Controller controller = method.getAnnotation(Controller.class);
                EventType[] eventTypes = controller.events();
                String pattern = controller.pattern();
                String next = controller.next();

                if (!StringUtils.isEmpty(next)) {
                    conversationMethodNames.add(next);
                }

                MethodWrapper methodWrapper = new MethodWrapper();
                methodWrapper.setMethod(method);
                methodWrapper.setPattern(pattern);
                methodWrapper.setNext(next);

                if (!conversationMethodNames.contains(method.getName())) {
                    for (EventType eventType : eventTypes) {
                        List<MethodWrapper> methodWrappers = eventToMethodsMap.get(eventType.name());

                        if (methodWrappers == null) {
                            methodWrappers = new ArrayList();
                        }

                        methodWrappers.add(methodWrapper);
                        eventToMethodsMap.put(eventType.name(), methodWrappers);
                    }
                }
                methodNameMap.put(method.getName(), methodWrapper);
            }
        }
    }
    
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.debug("WebSocket connected: {}", session);
    }
    
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        logger.debug("WebSocket closed: {}, Close Status: {}", session, status.toString());
    }
    
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("Transport Error: {}", exception);
    }
    
     public final void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Event event = mapper.readValue(textMessage.getPayload(), Event.class);
            if (event.getType() != null) {
                if (event.getType().equalsIgnoreCase(EventType.IM_OPEN.name())) {
                    slackService.addDmChannel(event.getChannelId());
                } else if (event.getType().equalsIgnoreCase(EventType.MESSAGE.name())) {
                    if (event.getText() != null && event.getText().contains(slackService.getCurrentUser().getId())) { // direct mention
                        event.setType(EventType.DIRECT_MENTION.name());
                    } else if (slackService.getDmChannels().contains(event.getChannelId())) { // direct message
                        event.setType(EventType.DIRECT_MESSAGE.name());
                    }
                }
            } else { // Slack không gửi bất kỳ TYPE nào cho các thông báo xác nhận
                event.setType(EventType.ACK.name());
            }

            if (isConversationOn(event)) {
                invokeChainedMethod(session, event);
            } else {
                invokeMethods(session, event);
            }
        } catch (Exception e) {
            logger.error("Error handling response from Slack: {}. \nException: ", textMessage.getPayload(), e);
        }
    }
     
    public void startConversation(Event event, String methodName) {
        String channelId = event.getChannelId();

        if (!StringUtils.isEmpty(channelId)) {
            Queue<MethodWrapper> queue = formConversationQueue(new LinkedList(), methodName);
            conversationQueueMap.put(channelId, queue);
        }
    }
     
    public void nextConversation(Event event) {
        Queue<MethodWrapper> queue = conversationQueueMap.get(event.getChannelId());
        if (queue != null) queue.poll();
    }
    
    public void stopConversation(Event event) {
        conversationQueueMap.remove(event.getChannelId());
    }
    
    public boolean isConversationOn(Event event) {
        return conversationQueueMap.get(event.getChannelId()) != null;
    }
    
    public final void reply(WebSocketSession session, Event event, Message reply) {
        try {
            reply.setType(EventType.MESSAGE.name().toLowerCase());
            reply.setText(encode(reply.getText()));
            if (reply.getChannel() == null && event.getChannelId() != null) {
                reply.setChannel(event.getChannelId());
            }
            session.sendMessage(new TextMessage(reply.toJSONString()));
            if (logger.isDebugEnabled()) {  // For debugging purpose only
                logger.debug("Reply (Message): {}", reply.toJSONString());
            }
        } catch (IOException e) {
            logger.error("Error sending event: {}. Exception: {}", event.getText(), e.getMessage());
        }
    }
    
    private String encode(String message) {
        return message == null ? null : message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
    
    private Queue<MethodWrapper> formConversationQueue(Queue<MethodWrapper> queue, String methodName) {
        MethodWrapper methodWrapper = methodNameMap.get(methodName);
        queue.add(methodWrapper);
        if (StringUtils.isEmpty(methodName)) {
            return queue;
        } else {
            return formConversationQueue(queue, methodWrapper.getNext());
        }
    }
    
    private void invokeMethods(WebSocketSession session, Event event) {
        try {
            List<MethodWrapper> methodWrappers = eventToMethodsMap.get(event.getType().toUpperCase());
            if (methodWrappers == null) return;

            methodWrappers = new ArrayList(methodWrappers);
            MethodWrapper matchedMethod = getMethodWithMatchingPatternAndFilterUnmatchedMethods(event, methodWrappers);
            if (matchedMethod != null) {
                methodWrappers = new ArrayList();
                methodWrappers.add(matchedMethod);
            }

            if (methodWrappers != null) {
                for (MethodWrapper methodWrapper : methodWrappers) {
                    Method method = methodWrapper.getMethod();
                    if (method.getParameterCount() == 3) {
                        method.invoke(this, session, event, methodWrapper.getMatcher());
                    } else {
                        method.invoke(this, session, event);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error invoking controller: ", e);
        }
    }
    
    private void invokeChainedMethod(WebSocketSession session, Event event) {
        Queue<MethodWrapper> queue = conversationQueueMap.get(event.getChannelId());

        if (queue != null && !queue.isEmpty()) {
            MethodWrapper methodWrapper = queue.peek();

            try {
                EventType[] eventTypes = methodWrapper.getMethod().getAnnotation(Controller.class).events();
                for (EventType eventType : eventTypes) {
                    if (eventType.name().equals(event.getType().toUpperCase())) {
                        methodWrapper.getMethod().invoke(this, session, event);
                        return;
                    }
                }
            } catch (Exception e) {
                logger.error("Error invoking chained method: ", e);
            }
        }
    }
    
    private MethodWrapper getMethodWithMatchingPatternAndFilterUnmatchedMethods(Event event, List<MethodWrapper> methodWrappers) {
        if (methodWrappers != null) {
            Iterator<MethodWrapper> listIterator = methodWrappers.listIterator();

            while (listIterator.hasNext()) {
                MethodWrapper methodWrapper = listIterator.next();
                String pattern = methodWrapper.getPattern();
                String text = event.getText();

                if (!StringUtils.isEmpty(pattern) && !StringUtils.isEmpty(text)) {
                    Pattern p = Pattern.compile(pattern);
                    Matcher m = p.matcher(text);
                    if (m.find()) {
                        methodWrapper.setMatcher(m);
                        return methodWrapper;
                    } else {
                        listIterator.remove();  // remove methods from the original list whose pattern do not match
                    }
                }
            }
        }
        return null;
    }
    
    private StandardWebSocketClient client() {
        return new StandardWebSocketClient();
    }

    private BotWebSocketHandler handler() {
        return new BotWebSocketHandler(getSlackBot());
    }
    
    @PostConstruct
    private void startWebSocketConnection() {
        slackService.startRTM(getSlackToken());
        if (slackService.getWebSocketUrl() != null) {
            WebSocketConnectionManager manager = new WebSocketConnectionManager(client(), handler(), slackService.getWebSocketUrl());
            manager.start();
        } else {
            logger.error("No websocket url returned by Slack.");
        }
    }
    
    private class MethodWrapper {
        private Method method;
        private String pattern;
        private Matcher matcher;
        private String next;

        public Method getMethod() {
            return method;
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public Matcher getMatcher() {
            return matcher;
        }

        public void setMatcher(Matcher matcher) {
            this.matcher = matcher;
        }

        public String getNext() {
            return next;
        }

        public void setNext(String next) {
            this.next = next;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MethodWrapper that = (MethodWrapper) o;

            if (!method.equals(that.method)) return false;
            if (pattern != null ? !pattern.equals(that.pattern) : that.pattern != null) return false;
            if (matcher != null ? !matcher.equals(that.matcher) : that.matcher != null) return false;
            return next != null ? next.equals(that.next) : that.next == null;

        }

        @Override
        public int hashCode() {
            int result = method.hashCode();
            result = 31 * result + (pattern != null ? pattern.hashCode() : 0);
            result = 31 * result + (matcher != null ? matcher.hashCode() : 0);
            result = 31 * result + (next != null ? next.hashCode() : 0);
            return result;
        }
    }
    
}
