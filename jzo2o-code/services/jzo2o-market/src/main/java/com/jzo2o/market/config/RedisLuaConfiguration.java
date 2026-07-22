package com.jzo2o.market.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
public class RedisLuaConfiguration {

    @Bean("seizeCouponScript")
    public DefaultRedisScript<Integer> seizeCouponScript() {
        DefaultRedisScript<Integer> redisScript = new DefaultRedisScript<>();
        //resource目录下的scripts文件下的seizeCouponScript.lua文件
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/seizeCouponScript.lua")));
        redisScript.setResultType(Integer.class);
        return redisScript;
    }

    @Bean("lua_test01")
    public DefaultRedisScript<Integer> getLuaTest01() {
        DefaultRedisScript<Integer> redisScript = new DefaultRedisScript<>();
        //resource目录下的scripts文件下的lua_test01.lua文件
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/lua_test01.lua")));
        redisScript.setResultType(Integer.class);
        return redisScript;
    }

}
