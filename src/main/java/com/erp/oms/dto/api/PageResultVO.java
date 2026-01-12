package com.erp.oms.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "分页返回结果包装")
public class PageResultVO<T> {

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "列表数据")
    private List<T> list;

    /**
     * 静态转换方法，方便在 Service 层调用
     */
    public static <T> PageResultVO<T> of(Long total, List<T> list) {
        return new PageResultVO<>(total, list);
    }
}