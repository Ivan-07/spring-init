package com.ivan.project.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.ivan.project.model.entity.User;
import com.ivan.project.model.vo.UserVO;
import com.ivan.project.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author ivan
 */
@Component
@Slf4j
public class PreCacheJob {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    // 重点用户
    private List<Long> mainUserList = Arrays.asList(3L);

    // 每天执行，预热用户列表
    @Scheduled(cron = "0 4 0 * * *")
    public void doCacheUserPage() {
        RLock lock = redissonClient.getLock("ivan:precachejob:docache:lock");
        try {
            if (lock.tryLock(0, 30000L, TimeUnit.MILLISECONDS)) {
                for (Long userId : mainUserList) {
                    long current = 1;
                    long size = 10;
                    String redisKey = "ivan:user:page_" + current + "_size_" + size;
                    ValueOperations valueOperations = redisTemplate.opsForValue();
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                    Page<User> userPage = userService.page(new Page<>(current, size), queryWrapper);
                    Page<UserVO> userVOPage = new PageDTO<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
                    List<UserVO> userVOList = userPage.getRecords().stream().map(user -> {
                        UserVO userVO = new UserVO();
                        BeanUtils.copyProperties(user, userVO);
                        return userVO;
                    }).collect(Collectors.toList());
                    userVOPage.setRecords(userVOList);
                    // 写缓存
                    try {
                        valueOperations.set(redisKey, userVOPage);
                    } catch (Exception e) {
                        log.error("redis set key error", e);
                    }
                }

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
