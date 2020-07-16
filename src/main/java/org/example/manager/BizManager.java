package org.example.manager;

import org.example.pojo.TransactionReqVO;

import java.util.List;

public interface BizManager {

    boolean transactionMoney(List<TransactionReqVO> transactionReqVOList) throws Exception;
}
