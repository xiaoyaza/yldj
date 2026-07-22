package com.jzo2o.foundations.controller.consumer;

import com.jzo2o.foundations.model.dto.response.ServeCategoryResDTO;
import com.jzo2o.foundations.model.dto.response.ServeAggregationSimpleResDTO;
import com.jzo2o.foundations.model.dto.response.ServeAggregationTypeSimpleResDTO;
import com.jzo2o.foundations.service.HomeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController("consumerServeController")
@RequestMapping("/customer/serve")
@Api(tags = "用户端 - 首页服务查询接口")
public class FirstPageServeController {
    @Resource
    private HomeService homeService;


    @GetMapping("/firstPageServeList")
    @ApiOperation("首页服务列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "regionId", value = "区域id", required = true, dataTypeClass = Long.class)
    })
    public List<ServeCategoryResDTO> serveCategory(@RequestParam("regionId") Long regionId) {
        List<ServeCategoryResDTO> serveCategoryResDTOS = homeService.queryServeIconCategoryByRegionIdCache(regionId);
        return serveCategoryResDTOS;
    }

    @GetMapping("/hotServeList")
    @ApiOperation("首页热门服务列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "regionId", value = "区域id", required = true, dataTypeClass = Long.class)
    })
    public List<ServeAggregationSimpleResDTO> hotServeList(@RequestParam("regionId") Long regionId) {
        return homeService.queryHotServeListByRegionId(regionId);
    }

    @GetMapping("/search")
    @ApiOperation("服务搜索")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "cityCode", value = "城市编码", required = true, dataTypeClass = String.class),
            @ApiImplicitParam(name = "serveTypeId", value = "服务类型id", required = true, dataTypeClass = Long.class)
    })
    public List<ServeAggregationSimpleResDTO> search(@RequestParam("cityCode") String cityCode,
                                                     @RequestParam("serveTypeId") Long serveTypeId) {
        return homeService.queryServeListByCityCodeAndServeTypeId(cityCode, serveTypeId);
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询服务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "服务id", required = true, dataTypeClass = Long.class)
    })
    public ServeAggregationSimpleResDTO queryServeById(@PathVariable("id") Long id) {
        return homeService.queryServeById(id);
    }

    @GetMapping("/serveTypeList")
    @ApiOperation("服务分类列表")
    public List<ServeAggregationTypeSimpleResDTO> serveTypeList(@RequestParam("regionId") Long regionId) {
        //Service Category List
        return homeService.queryServeCategoryListByRegionId(regionId);
    }

}
