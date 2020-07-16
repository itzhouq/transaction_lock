package org.example.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class TestTableDO implements Serializable {

    private Long id;

    private Long userId;

    private Long money;
}
