package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.manager.BizManager;
import org.example.pojo.TransactionReqVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@RestController
@RequestMapping("demo")
@Slf4j
public class DemoController {

    @Autowired
    private BizManager bizManager;

    /**
     * 消息接收，同一秒进来两个不同的消息，但是是其中包含两个相同的 userId：4、5
     */
    @GetMapping("/consumer")
    public void consumer() {
        bizManager.testData();
    }
}
