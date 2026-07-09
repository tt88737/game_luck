package com.gameluck.wallet.client.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class ClientPageVo<T> {
    private List<T> records;
    private Long total;
}
