package org.example.service;

import org.example.pojo.TransactionReqVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface TestTableService {

    int update(long userId, long money);

    List<TransactionReqVO> selectAll();

    void updateTableData(TransactionReqVO transactionReqVO);
}
