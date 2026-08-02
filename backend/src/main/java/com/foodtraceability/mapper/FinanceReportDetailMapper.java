package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.FinanceReportDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface FinanceReportDetailMapper extends BaseMapper<FinanceReportDetail> {
    
    List<FinanceReportDetail> selectByReportId(@Param("reportId") Long reportId);
    
    List<FinanceReportDetail> selectByReportIdAndType(@Param("reportId") Long reportId, @Param("itemType") String itemType);
    
    void deleteByReportId(@Param("reportId") Long reportId);
}
