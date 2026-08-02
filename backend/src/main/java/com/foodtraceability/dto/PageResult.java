package com.foodtraceability.dto;

import java.util.List;

/**
 * 分页结果封装类
 * 用于封装分页查询的结果
 * 同时支持前端期望的字段名(page, pageSize)和MyBatis Plus的字段名(current, size)
 */
public class PageResult<T> {
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 当前页数据列表
     */
    private List<T> records;
    
    /**
     * 当前页码（MyBatis Plus字段名）
     */
    private Long current;
    
    /**
     * 每页大小（MyBatis Plus字段名）
     */
    private Long size;
    
    /**
     * 无参构造方法
     */
    public PageResult() {
        this.total = 0L;
        this.records = null;
        this.current = 1L;
        this.size = 10L;
    }

    /**
     * 构造方法
     * @param total 总记录数
     * @param records 当前页数据列表
     */
    public PageResult(Long total, List<T> records) {
        this.total = total;
        this.records = records;
        this.current = 1L;
        this.size = 10L;
    }
    
    /**
     * 构造方法
     * @param total 总记录数
     * @param records 当前页数据列表
     * @param current 当前页码
     * @param size 每页大小
     */
    public PageResult(Long total, List<T> records, Long current, Long size) {
        this.total = total;
        this.records = records;
        this.current = current;
        this.size = size;
    }
    
    /**
     * 获取总记录数
     * @return 总记录数
     */
    public Long getTotal() {
        return total;
    }
    
    /**
     * 设置总记录数
     * @param total 总记录数
     */
    public void setTotal(Long total) {
        this.total = total;
    }
    
    /**
     * 获取当前页数据列表
     * @return 当前页数据列表
     */
    public List<T> getRecords() {
        return records;
    }
    
    /**
     * 设置当前页数据列表
     * @param records 当前页数据列表
     */
    public void setRecords(List<T> records) {
        this.records = records;
    }
    
    /**
     * 获取当前页码（MyBatis Plus字段名）
     * @return 当前页码
     */
    public Long getCurrent() {
        return current;
    }
    
    /**
     * 设置当前页码（MyBatis Plus字段名）
     * @param current 当前页码
     */
    public void setCurrent(Long current) {
        this.current = current;
    }
    
    /**
     * 获取每页大小（MyBatis Plus字段名）
     * @return 每页大小
     */
    public Long getSize() {
        return size;
    }
    
    /**
     * 设置每页大小（MyBatis Plus字段名）
     * @param size 每页大小
     */
    public void setSize(Long size) {
        this.size = size;
    }
    
    /**
     * 获取当前页码（前端期望的字段名）
     * @return 当前页码
     */
    public Long getPage() {
        return current;
    }
    
    /**
     * 设置当前页码（前端期望的字段名）
     * @param page 当前页码
     */
    public void setPage(Long page) {
        this.current = page;
    }
    
    /**
     * 获取每页大小（前端期望的字段名）
     * @return 每页大小
     */
    public Long getPageSize() {
        return size;
    }
    
    /**
     * 设置每页大小（前端期望的字段名）
     * @param pageSize 每页大小
     */
    public void setPageSize(Long pageSize) {
        this.size = pageSize;
    }
}