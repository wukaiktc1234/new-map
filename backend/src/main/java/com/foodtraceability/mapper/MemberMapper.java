package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {
    
    @Select("SELECT * FROM members WHERE phone = #{phone}")
    Member findByPhone(String phone);
    
    @Select("SELECT * FROM members WHERE member_no = #{memberNo}")
    Member findByMemberNo(String memberNo);
    
    @Select("SELECT * FROM members WHERE status = 1 ORDER BY total_consume DESC LIMIT #{limit}")
    List<Member> findTopMembers(int limit);

    @org.apache.ibatis.annotations.Update("UPDATE members SET balance = balance - #{amount}, total_consume = COALESCE(total_consume, 0) + #{amount}, order_count = COALESCE(order_count, 0) + 1, last_visit_time = NOW(), update_time = NOW() WHERE member_no = #{memberNo} AND balance >= #{amount} AND #{amount} > 0 AND deleted = 0")
    int deductBalance(@Param("memberNo") String memberNo, @Param("amount") BigDecimal amount);
}
