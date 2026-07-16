package com.jzo2o.foundations.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.common.model.dto.PageQueryDTO;
import com.jzo2o.foundations.model.domain.Serve;
import com.jzo2o.foundations.model.dto.request.ServePageQueryReqDTO;
import com.jzo2o.foundations.model.dto.request.ServeUpsertReqDTO;
import com.jzo2o.foundations.model.dto.response.ServeResDTO;

import java.math.BigDecimal;
import java.util.List;

public interface IServeService extends IService<Serve> {

    /**
     * 分页查询服务列表
     * @param servePageQueryReqDTO
     * @return
     */
    PageResult<ServeResDTO> page(ServePageQueryReqDTO servePageQueryReqDTO);

    /**
     * 批量新增
     * @param serveUpsertReqDTOList 批量新增数据
     */
    void batchAdd(List<ServeUpsertReqDTO> serveUpsertReqDTOList);

    /**
     * 修改服务价格
     * @param id
     * @param price
     */
    Serve update(Long id, BigDecimal price);

    /**
     * 上架
     * @param id
     */
    Serve onSale(Long id);

    /**
     * 根据id删除区域服务
     * @param id
     * @return
     */
    void deleteById(Long id);

    /**
     * 下架
     * @param id
     */
    void offsale(Long id);

    /**
     * 区域服务设置热门
     * @param id
     */
    void onHot(Long id);

    /**
     * 区域服务取消热门
     * @param id
     */
    void offHot(Long id);

    /**
     * 查询指定服务状态数量
     */
    Long queryServeCountByRegionIdAndSaleStatus(Long regionId, Integer saleStatus);

    /**
     * 查询指定服务项状态数量
     * @param serveItemId
     * @param saleStatus
     * @return
     */
    Long queryServeCountByServeItemIdAndSaleStatus(Long serveItemId, Integer saleStatus);
}
