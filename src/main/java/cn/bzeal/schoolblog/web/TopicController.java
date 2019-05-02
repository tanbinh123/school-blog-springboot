package cn.bzeal.schoolblog.web;

import cn.bzeal.schoolblog.model.QueryModel;
import cn.bzeal.schoolblog.service.TopicService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/topic")
public class TopicController extends BaseController {

    private final TopicService topicService;

    @Autowired
    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    // 新增话题
    @RequestMapping("/add")
    public String add(QueryModel model) {
        String userid = getRequest().getAttribute("uid").toString();
        // TODO 在这里判断角色在不增加角色变更功能的时候没有问题，如有增加则需要注意角色信息过期问题
        Integer role = (Integer) getRequest().getAttribute("role");
        String name = model.getTopic().getName();
        String summary = model.getTopic().getSummary();
        // 必须有前三个参数且角色必须是教师以上
        if (StringUtils.isAnyBlank(userid, name, summary) || role == null || role < 1) {
            return defaultResult();
        }
        return topicService.add(model, Long.parseLong(userid));
    }

    // 话题列表
    @RequestMapping("/lstById")
    public String lstById() {
        String userid = getRequest().getAttribute("uid").toString();
        if (StringUtils.isBlank(userid)) {
            return defaultResult();
        }
        return topicService.lstById(Long.parseLong(userid));
    }

    // 根据创建者获取话题列表
    @RequestMapping("/lstByCreator")
    public String lstByCreator(QueryModel model) {
        if (model.getUser() == null || model.getUser().getId() == null) {
            return defaultResult();
        }
        return topicService.lstByCreator(model);
    }

    // 根据id查询话题
    @RequestMapping("/find")
    public String find(QueryModel model) {
        if (model.getTopic() == null || model.getTopic().getId() == null) {
            return defaultResult();
        }
        String userId = getRequest().getAttribute("uid").toString();
        return topicService.find(model.getTopic().getId(), Long.parseLong(userId));
    }

    // 用户相关话题（创建、加入）
    @RequestMapping("/lstAboutId")
    public String lstAboutId() {
        String userid = getRequest().getAttribute("uid").toString();
        if (StringUtils.isBlank(userid)) {
            return defaultResult();
        }
        return topicService.lstAboutId(Long.parseLong(userid));
    }

    // 根据用户id查询计入的话题列表
    @RequestMapping("lstByFollower")
    public String lstByFollower(QueryModel model) {
        if (model.getUser() == null || model.getUser().getId() == null) {
            return defaultResult();
        }
        return topicService.lstByFollower(model);
    }

    // 全部话题
    @RequestMapping("/lst")
    public String lst(QueryModel model) {
        return topicService.lst(model);
    }

    // 加入话题
    @RequestMapping("/follow")
    public String follow(QueryModel model){
        String userId = getRequest().getAttribute("uid").toString();
        if (model.getTopic() == null || model.getTopic().getId() == null) {
            return defaultResult();
        }
        return topicService.follow(model.getTopic().getId(), Long.parseLong(userId));
    }
}
