package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.JobOffer;
import org.apache.ibatis.annotations.Mapper;

/**
 * 录用Offer Mapper 接口
 * 对应表: job_offers
 */
@Mapper
public interface JobOfferMapper extends BaseMapper<JobOffer> {
}
