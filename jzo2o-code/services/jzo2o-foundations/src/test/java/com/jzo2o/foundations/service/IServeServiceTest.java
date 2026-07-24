package com.jzo2o.foundations.service;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.foundations.model.domain.Serve;
import com.jzo2o.foundations.model.dto.request.ServePageQueryReqDTO;
import com.jzo2o.foundations.model.dto.response.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
@Slf4j
class IServeServiceTest {

    @Resource
    private IServeService serveService;

    //区域服务查询
    @Test
    public void test_queryServeByIdCache() {
        Serve serve = serveService.queryServeByIdCache(1693815623867506689L);
        Assert.notNull(serve, "服务为空");
    }
}