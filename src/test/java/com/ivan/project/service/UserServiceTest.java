package com.ivan.project.service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import com.alibaba.fastjson.JSON;

import com.ivan.project.model.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;

/**
 * 用户服务测试
 *
 * @author ivan
 */
@SpringBootTest
class UserServiceTest {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;

    @Test
    void testAddUser() {
        User user = new User();
        boolean result = userService.save(user);
        System.out.println(user.getId());
        Assertions.assertTrue(result);
    }

    @Test
    void testUpdateUser() {
        User user = new User();
        boolean result = userService.updateById(user);
        Assertions.assertTrue(result);
    }

    @Test
    void testDeleteUser() {
        boolean result = userService.removeById(1L);
        Assertions.assertTrue(result);
    }

    @Test
    void testGetUser() {
        User user = userService.getById(1L);
        Assertions.assertNotNull(user);
    }

    @Test
    void userRegister() {
        String userAccount = "ivan";
        String userPassword = "";
        String checkPassword = "123456";
        try {
            long result = userService.userRegister(userAccount, userPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            userAccount = "yu";
            result = userService.userRegister(userAccount, userPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            userAccount = "ivan";
            userPassword = "123456";
            result = userService.userRegister(userAccount, userPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            userAccount = "yu pi";
            userPassword = "12345678";
            result = userService.userRegister(userAccount, userPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            checkPassword = "123456789";
            result = userService.userRegister(userAccount, userPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            userAccount = "dogivan";
            checkPassword = "12345678";
            result = userService.userRegister(userAccount, userPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            userAccount = "ivan";
            result = userService.userRegister(userAccount, userPassword, checkPassword);
            Assertions.assertEquals(-1, result);
        } catch (Exception e) {

        }
    }

    @Test
    void testRedis() {
        User user = new User();
        user.setId(1L);
        user.setUserName("test");
        user.setUserAccount("test");
        user.setUserAvatar("test");
        user.setGender(0);
        user.setUserRole("test");
        user.setUserPassword("12345678");
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        user.setIsDelete(0);

        HashOperations<String, String, String> map = redisTemplate.opsForHash();

        //设置100 seconds存活时间
        redisTemplate.expire(user.getId(), 10, TimeUnit.MINUTES);

        for (int i = 0; i < 20; i++) {
            map.put("hash"+ i, "userName", user.getUserName());
            map.put("hash"+i, "userAccount", user.getUserAccount());
            map.put("hash"+i, "userAvatar", user.getUserAvatar());
            map.put("hash"+i, "gender", String.valueOf(user.getGender()));
            map.put("hash"+i, "userRole", user.getUserRole());
            map.put("hash"+i, "userPassword", user.getUserPassword());
            map.put("hash"+i, "createTime", user.getCreateTime().toString());
            map.put("hash"+i, "updateTime", user.getUpdateTime().toString());
        }

//        for (int i = 0; i < 20; i++) {
//            redisTemplate.opsForValue().set("string"+i, JSON.toJSONString(user));
//        }

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            String user1 = (String) redisTemplate.opsForValue().get("string0");
            User user2 = JSON.parseObject(user1, User.class);
            String userName = user2.getUserName();
        }
        long endTime = System.currentTimeMillis();
        System.out.println("String程序运行时间：" + (endTime - startTime) + "ms");    //输出程序运行时间

        startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            String userName = (String) redisTemplate.opsForHash().get("hash0", "userName");
        }
        endTime = System.currentTimeMillis();
        System.out.println("Hash程序运行时间：" + (endTime - startTime) + "ms");    //输出程序运行时间




//        Map stumap=redisTemplate.boundHashOps("2").entries();
//        System.out.println("map对象："+stumap);
//        System.out.println(stumap.get("name"));
    }

}