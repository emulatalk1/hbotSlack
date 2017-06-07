package core.bot.slack;

import core.bot.models.RTM;
import core.bot.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Spectre
 */
@Service
@Scope("prototype")
public class SlackService {

    @Autowired
    private SlackDao slackDao;
    private User currentUser;
    private List<String> dmChannels;
    private String webSocketUrl;

    /* 
    Bắt đầu kết nối RTM. Tìm nạp url web socket để kết nối đến, thông tin user hiện tại
    và danh sách id kênh nơi user hiện tại đã trò chuyện.
   
    @param slackToken 
    */     
    public void startRTM(String slackToken) {
        RTM rtm = slackDao.startRTM(slackToken);
        currentUser = rtm.getUser();
        dmChannels = rtm.getDmChannels();
        webSocketUrl = rtm.getWebSocketUrl();
    }

    //@return user đại diện cho bot.
    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    /*
     @return danh sách id các channel mà user đã trò chuyện .
     */ 
    public List<String> getDmChannels() {
        return dmChannels;
    }

    public void setDmChannels(List<String> dmChannels) {
        this.dmChannels = dmChannels;
    }

    public boolean addDmChannel(String channelId) {
        if (dmChannels == null) dmChannels = new ArrayList();
        return dmChannels.add(channelId);
    }

    /*
     @return web socket url để kết nối tới.
     */
    public String getWebSocketUrl() {
        return webSocketUrl;
    }

    public void setWebSocketUrl(String webSocketUrl) {
        this.webSocketUrl = webSocketUrl;
    }
}
