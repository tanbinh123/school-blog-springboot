package cn.bzeal.schoolblog.service.impl;

import cn.bzeal.schoolblog.common.AppConst;
import cn.bzeal.schoolblog.common.GlobalResult;
import cn.bzeal.schoolblog.common.ResponseCode;
import cn.bzeal.schoolblog.domain.User;
import cn.bzeal.schoolblog.domain.UserRepository;
import cn.bzeal.schoolblog.model.QueryModel;
import cn.bzeal.schoolblog.service.UserService;
import cn.bzeal.schoolblog.util.JwtTokenUtil;
import cn.bzeal.schoolblog.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    public String login(QueryModel model) {
        User user = userRepository.findByIdAndPassword(model.getUser().getId(), model.getUser().getPassword());
        // 判断用户信息 有则生成 token 返回
        if (user != null) {
            try {
                String token = JwtTokenUtil.createToken(user.getId().toString(), user.getRole(), user.getName());
                HashMap<String, Object> data = new HashMap<>();
                data.put("token", token);
                data.put("user", user);
                data.put("expires", 60 * 60 * 24 * 14); // 超时时间为两周
                return ResponseUtil.revertUser(ResponseUtil.getResultMap(ResponseCode.T_USER_SUCCESS_LOGIN, data));
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseUtil.getResult(ResponseCode.T_USER_FAIL_LOGIN);
            }
        } else {
            return ResponseUtil.getResult(ResponseCode.T_USER_EMPTY_FIND);
        }
    }

    // 查询用户信息
    @Override
    public String getInfo(Long username) {
        User user = userRepository.findById(username).orElse(null);
        // 判断用户信息 有则生成 token 返回
        if (user != null) {
            try {
                HashMap<String, Object> data = new HashMap<>();
                data.put("user", user);
                return ResponseUtil.revertUser(ResponseUtil.getResultMap(ResponseCode.N_SUCCESS, data));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ResponseUtil.getResult(ResponseCode.T_USER_EMPTY_FIND);
    }

    @Override
    public String countUser(Long userid, Long currentUserId) {
        User user = userRepository.findById(userid).orElse(null);
        User currentUser = userRepository.findById(currentUserId).orElse(null);
        if (user != null && currentUser != null) {
            HashMap<String, Object> data = new HashMap<>();
            data.put("wzcount", user.getArticles().size());
            data.put("sccount", user.getFavs().size());
            data.put("htcount", user.getTopics().size() + user.getFollows().size());
            data.put("fscount", user.getBefws().size());
            data.put("isfollow", user.getBefws().contains(currentUser));
            return ResponseUtil.revert(ResponseUtil.getResultMap(ResponseCode.N_SUCCESS, data));
        }
        return ResponseUtil.getResult(ResponseCode.T_USER_EMPTY_FIND);
    }

    @Override
    public String insertUser(QueryModel model) {
        List<String> querys = model.getQueryList();
        User user = new User();
        user.setName(querys.get(0));
        user.setCollege(querys.get(1));
        user.setTel(querys.get(2));
        user.setRole(Integer.parseInt(querys.get(3)));
        user.setReg(new Timestamp(System.currentTimeMillis()));
        user.setPassword("admin");
        // TODO 默认密码应该修改为其他密码
        if (userRepository.save(user) != null) {
            return ResponseUtil.getResult(ResponseCode.T_APP_SUCCESS_ADD);
        } else {
            return ResponseUtil.getResult(ResponseCode.T_APP_FAIL_SAVE);
        }
    }

    @Override
    public String followOrNot(QueryModel model, Long currentUserId) {
        // 不允许自己关注自己
        if (model.getUser().getId().equals(currentUserId)) {
            return ResponseUtil.getResult(ResponseCode.T_USER_CONFLICT_FOLLOW);
        }
        User currentUser = userRepository.findById(currentUserId).orElse(null);
        User targetUser = userRepository.findById(model.getUser().getId()).orElse(null);
        if (currentUser != null && targetUser != null) {
            if (currentUser.getTofws().contains(targetUser)) {
                if (model.getQueryType() == AppConst.USER_FOLLOW_CANCEL) {
                    currentUser.getTofws().remove(targetUser);
                    targetUser.getBefws().remove(currentUser);
                }
            } else if (model.getQueryType() == AppConst.USER_FOLLOW) {
                currentUser.getTofws().add(targetUser);
                targetUser.getBefws().add(currentUser);
            }
            userRepository.save(currentUser);
            userRepository.save(targetUser);
            return ResponseUtil.getResult(ResponseCode.T_USER_SUCCESS_FOLLOW);
        }
        return ResponseUtil.getResult(ResponseCode.T_USER_EMPTY_FIND);
    }

    @Override
    public String lst(QueryModel model, HttpServletRequest request) {
        // 定义分页，获取全部用户
        // TODO Page size must not be less than one!添加该异常验证
        Pageable pageable = PageRequest.of(model.getPage(), model.getRow());
        List<User> list = new ArrayList<>();
        long totalPage = 0L;
        if (model.getQueryType() == AppConst.QUERY_USERLIST_NORMAL) {
            Page<User> page = userRepository.findAllByIdNot(Long.parseLong((String) request.getAttribute("uid")), pageable);
            totalPage = page.getTotalElements();
            list = page.getContent();
        } else if (model.getQueryType() == AppConst.QUERY_USERLIST_USERNAME) {
            // 此处一定只有一个数据或者没有数据
            Long id = model.getUser().getId();
            userRepository.findById(id).ifPresent(list::add);
            totalPage = (long) list.size();
        } else if (model.getQueryType() >= AppConst.QUERY_USERLIST_Q) {
            User user = model.getUser();
            user.setName("%" + (user.getName() == null ? "" : user.getName()) + "%");
            user.setCollege("%" + (user.getCollege() == null ? "" : user.getCollege()) + "%");
            Page<User> page = userRepository.findAllByNameLikeAndCollegeLike(user.getName(), user.getCollege(), pageable);
            totalPage = page.getTotalElements();
            list = page.getContent();
        }
        HashMap<String, Object> data = new HashMap<>();
        data.put("lst", list);
        data.put("total", totalPage);
        return ResponseUtil.revertUser(ResponseUtil.getResultMap(ResponseCode.N_SUCCESS, data));
    }

    @Override
    public String deleteUser(Long userid) {
        userRepository.deleteById(userid);
        return ResponseUtil.getResult(ResponseCode.T_APP_SUCCESS_DELETE);
    }

}
