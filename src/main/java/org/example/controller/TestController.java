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
@RequestMapping("test")
@Slf4j
public class TestController {

    @Autowired
    private BizManager bizManager;

    /**
     * 消息接收，同一秒进来两个不同的消息，但是是其中包含两个相同的 userId：4、5
     */
    @GetMapping("/consumer")
    public void consumer() {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        Thread consumerThread1 = new Thread(() -> {
            List<TransactionReqVO> transactionReqVOList = new ArrayList<>();
            transactionReqVOList.add(TransactionReqVO.builder().money(5L).userId(5L).build());
            transactionReqVOList.add(TransactionReqVO.builder().money(1L).userId(1L).build());
            transactionReqVOList.add(TransactionReqVO.builder().money(4L).userId(4L).build());
            try {
                log.info("全局链路跟踪id:1的日志：{}", transactionReqVOList);
                bizManager.transactionMoney(transactionReqVOList);
            } catch (Exception e) {
                log.error("全局链路跟踪id:1的异常：{}", e.getMessage(), e);
            } finally {
                countDownLatch.countDown();
            }
        }, "ConsumerThread1");
        Thread consumerThread2 = new Thread(() -> {
            List<TransactionReqVO> transactionReqVOList = new ArrayList<>();
            transactionReqVOList.add(TransactionReqVO.builder().money(4L).userId(4L).build());
            transactionReqVOList.add(TransactionReqVO.builder().money(2L).userId(2L).build());
            transactionReqVOList.add(TransactionReqVO.builder().money(5L).userId(5L).build());
            try {
                log.info("全局链路跟踪id:2的日志：{}", transactionReqVOList);
                bizManager.transactionMoney(transactionReqVOList);
            } catch (Exception e) {
                log.error("全局链路跟踪id:2的异常：{}", e.getMessage(), e);
            } finally {
                countDownLatch.countDown();
            }
        }, "ConsumerThread2");
        try {
            consumerThread1.start();
            consumerThread2.start();
            countDownLatch.await();
        } catch (InterruptedException ignored) {
        }
    }
}
