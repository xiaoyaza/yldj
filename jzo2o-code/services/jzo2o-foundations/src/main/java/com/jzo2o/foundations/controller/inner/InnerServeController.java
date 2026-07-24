package com.jzo2o.foundations.controller.inner;

import com.jzo2o.api.foundations.dto.response.ServeAggregationResDTO;
import com.jzo2o.foundations.service.IServeService;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/inner/serve")
@Api(tags = "内部接口 - 区域相关接口")
public class InnerServeController {

    @Resource
    private IServeService serveService;

    /****
     * 查询服务、服务项、服务项分类信息
     */
    @GetMapping("/{id}")
    public ServeAggregationResDTO findById(@PathVariable("id")Long id){
        return serveService.findById(id);
    }
}