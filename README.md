# 线上死锁排查解决思路

主要是讲过程与思路，从日志反推故障现场，最后模拟和故障场景一模一样的代码。
<a name="DNMKp"></a>
# 1. 日志

<br />一个运行大半年的代码，突然在最近几天频出现死锁异常，业务机器大概每一两天发生一次如下的业务日志：
```
 INFO 57553 --- [ConsumerThread2] org.example.controller.TestController    : 全局链路跟踪id:2的日志：[TransactionReqVO(userId=4, money=4), TransactionReqVO(userId=2, money=2), TransactionReqVO(userId=5, money=5)]
 INFO 57553 --- [ConsumerThread1] org.example.controller.TestController    : 全局链路跟踪id:1的日志：[TransactionReqVO(userId=5, money=5), TransactionReqVO(userId=1, money=1), TransactionReqVO(userId=4, money=4)]
ERROR 57553 --- [ConsumerThread2] org.example.controller.TestController    : 全局链路跟踪id:2的异常：
### Error updating database.  Cause: com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction
### The error may exist in org/example/mapper/TestTableMapper.java (best guess)
### The error may involve org.example.mapper.TestTableMapper.update-Inline
### The error occurred while setting parameters
### SQL: UPDATE test_table SET money = money + ? WHERE user_id = ?
### Cause: com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction
; Deadlock found when trying to get lock; try restarting transaction; nested exception is com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction

org.springframework.dao.DeadlockLoserDataAccessException: 
### Error updating database.  Cause: com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction
### The error may exist in org/example/mapper/TestTableMapper.java (best guess)
### The error may involve org.example.mapper.TestTableMapper.update-Inline
### The error occurred while setting parameters
### SQL: UPDATE test_table SET money = money + ? WHERE user_id = ?
### Cause: com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction
; Deadlock found when trying to get lock; try restarting transaction; nested exception is com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException: Deadlock found when trying to get lock; try restarting transaction
	at org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator.doTranslate(SQLErrorCodeSQLExceptionTranslator.java:266) ~[spring-jdbc-5.0.13.RELEASE.jar:5.0.13.RELEASE]
	at org.springframework.jdbc.support.AbstractFallbackSQLExceptionTranslator.translate(AbstractFallbackSQLExceptionTranslator.java:72) ~[spring-jdbc-5.0.13.RELEASE.jar:5.0.13.RELEASE]
	at org.mybatis.spring.MyBatisExceptionTranslator.translateExceptionIfPossible(MyBatisExceptionTranslator.java:73) ~[mybatis-spring-2.0.1.jar:2.0.1]
	at org.mybatis.spring.SqlSessionTemplate$SqlSessionInterceptor.invoke(SqlSessionTemplate.java:446) ~[mybatis-spring-2.0.1.jar:2.0.1]
	at com.sun.proxy.$Proxy59.update(Unknown Source) ~[na:na]
	at org.mybatis.spring.SqlSessionTemplate.update(SqlSessionTemplate.java:294) ~[mybatis-spring-2.0.1.jar:2.0.1]
	at org.apache.ibatis.binding.MapperMethod.execute(MapperMethod.java:67) ~[mybatis-3.5.1.jar:3.5.1]
	at org.apache.ibatis.binding.MapperProxy.invoke(MapperProxy.java:58) ~[mybatis-3.5.1.jar:3.5.1]
	at com.sun.proxy.$Proxy62.update(Unknown Source) ~[na:na]
	at org.example.service.impl.TestServiceImpl.update(TestServiceImpl.java:16) ~[classes/:na]
	at org.example.manager.impl.BizManagerImpl.transactionMoney(BizManagerImpl.java:25) ~[classes/:na]
	at org.example.manager.impl.BizManagerImpl$$FastClassBySpringCGLIB$$824241b9.invoke(<generated>) ~[classes/:na]
```
`Deadlock` 非常显眼，说明业务上出现了死锁，肯定是业务上有问题。但是该业务代码一直运行了大半年，查看 Git 记录也发现最近没人动该业务相关代码，说明该业务之前就可能有问题，只是最近才达到了触发这种异常的条件。<br />关键点总结如下：

1. 这是什么错误日志？
> 从第  8~9 行可以得知，该错误是数据库的错误，是死锁错误异常而导致的回滚，关键 SQL 是： `UPDATE test_table SET money = money + ? WHERE user_id = ?` 

2. 核心错误的调用方法是哪个，即事务开始的方法是哪个？
> 过滤了 jdk 类、spring 类、mybatis 类后，得到核心的业务错误代码（30~31 行），31 行为 Spring 的代理执行，30 行才是真正最开始执行业务代码：BizManagerImpl.transactionMoney


<br />接着去查看该库对应的数据库死锁日志，使用命令： `show innodb engine status` ，过滤掉非重要的日志后如下：
```sql
------------------------
LATEST DETECTED DEADLOCK
------------------------
2020-07-14 19:34:29 0x7f958f1d5700
*** (1) TRANSACTION:
TRANSACTION 95146580, ACTIVE 2 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 4 lock struct(s), heap size 1136, 5 row lock(s), undo log entries 2
MySQL thread id 6264489, OS thread handle 140273305761536, query id 837446998 10.10.59.164 root updating
UPDATE test_table SET money = money + 5 WHERE user_id = 5
*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 71816 page no 4 n bits 80 index idx_user_id of table `mall`.`test_table` trx id 95146580 lock_mode X locks rec but not gap waiting
Record lock, heap no 3 PHYSICAL RECORD: n_fields 2; compact format; info bits 0
 0: len 8; hex 8000000000000005; asc         ;;
 1: len 8; hex 8000000000000006; asc         ;;

*** (2) TRANSACTION:
TRANSACTION 95146581, ACTIVE 2 sec starting index read
mysql tables in use 1, locked 1
4 lock struct(s), heap size 1136, 5 row lock(s), undo log entries 2
MySQL thread id 6264490, OS thread handle 140280327919360, query id 837446999 10.10.59.164 root updating
UPDATE test_table SET money = money + 4 WHERE user_id = 4
*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 71816 page no 4 n bits 80 index idx_user_id of table `mall`.`test_table` trx id 95146581 lock_mode X locks rec but not gap
Record lock, heap no 3 PHYSICAL RECORD: n_fields 2; compact format; info bits 0
 0: len 8; hex 8000000000000005; asc         ;;
 1: len 8; hex 8000000000000006; asc         ;;

Record lock, heap no 5 PHYSICAL RECORD: n_fields 2; compact format; info bits 0
 0: len 8; hex 8000000000000001; asc         ;;
 1: len 8; hex 8000000000000002; asc         ;;

*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 71816 page no 4 n bits 80 index idx_user_id of table `mall`.`test_table` trx id 95146581 lock_mode X locks rec but not gap waiting
Record lock, heap no 2 PHYSICAL RECORD: n_fields 2; compact format; info bits 0
 0: len 8; hex 8000000000000004; asc         ;;
 1: len 8; hex 8000000000000005; asc         ;;

*** WE ROLL BACK TRANSACTION (2)
```

<br />关键点总结如下：

1. 该库中最近一次死锁发生的时间是什么时候？
> 从第 4 行得知，最近一次死锁发生在 2020-07-14 19:34:29

2. 该次死锁导致的两个事务的重要信息？
> 12 行得知，事务 1 等待的锁为：lock_mode X locks rec but not gap waiting
> 24 行得知，事务 2 持有的锁为：lock_mode X locks rec but not gap
> 34 行得知，事务 2 等待的锁为：lock_mode X locks rec but not gap waiting
> 39 行得知，最后回滚的是事务 1
> 从 8、21、33 行得知：导致该次死锁的索引为： `idx_user_id` 

3. 能知道导致死锁的两个具体 SQL 吗？
> 不能，产生死锁的情况各式各样，事务中的 SQL 不止有两个 SQL，单从死锁日志是没法知道原因的，必须要结合业务代码查看事务上下文查看

<a name="aasLo"></a>
# 2. 理论知识

<br />这个问题需要尽快解决，而且排查发现有个特点，影响的都是是线上的**大用户**。排查前先去了解下相关的知识。<br />

<a name="AQTzn"></a>
## 2.1 死锁的条件


1. 互斥条件：一个资源每次只能被一个进程使用。
1. 占有且等待：一个进程因请求资源而阻塞时，对已获得的资源保持不放。
1. 不可强行占有：进程已获得的资源，在末使用完之前，不能强行剥夺。
1. 循环等待条件：若干进程之间形成一种头尾相接的循环等待资源关系。

破坏死锁也很简单，四个条件破一个即可。一般是破坏 2 或 4 条件。<br />

<a name="u9Gcp"></a>
## 2.2 数据库的锁类型

<br />数据库的死锁比较复杂，主要是由 Insert、Update（Delete 或 For Update 不考虑，因为实际业务场景我们是不会有 Delete 或 For Update 的操作的）。<br />InnoDB 的锁：

1. 共享锁与独占锁（S、X）
1. 意向锁
1. 记录锁（Record Locks）
1. 间隙锁（Gap Locks）
1. Next-Key Locks
1. 插入意向锁
1. 自增锁
1. 空间索引断言锁

这里参考了官网的 Innodb 锁分类（当时也没想到竟然有这么多种类的锁）。<br />

<a name="09hoN"></a>
# 3.  从死锁日志分析

<br />分析之前先要得到该表的建表语句： `show create table test_table;`  ：
```sql
CREATE TABLE `test_table` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `money` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8
```

<br />接着结合死锁日志、锁的种类、建表语句得出以下模糊的结论：

1. 从死锁日志的 10、12 行结合建表索引得知
> 事务1的 `UPDATE test_table SET money = money + 5 WHERE user_id = 5`  语句在等待锁：它通过普通索引 idx_user_id 更新，先获取了 user_id=5 的 X 锁，接着去申请对应行的主键（Record Lock）的行锁但是被阻塞（waiting），并不包括间隙锁（not gap）。具体是哪个主键我们并不清楚。

2. 从死锁日志的 22、24 行结合建表索引得知
> 事务2的 `UPDATE test_table SET money = money + 4 WHERE user_id = 4`  语句在持有锁：它通过普通索引 idx_user_id 更新，先获取了 user_id=4 的 X 锁，接着去申请对应行的主键（Record Lock）的行锁成功了，并不包括间隙锁（not gap）。具体是哪个主键我们并不清楚。

3. 从死锁日志的 22、34 行结合建表索引得知
> 事务2的 `UPDATE test_table SET money = money + 4 WHERE user_id = 4`  语句在等待锁：它通过普通索引 idx_user_id 更新，先获取了 user_id=4 的 X 锁，接着去申请对应行的主键（Record Lock）的行锁但是被阻塞（waiting），并不包括间隙锁（not gap）。具体是哪个主键我们并不清楚。

模糊结论肯定是有问题的，最大的问题在于 SQL 语句，即：死锁的原因是真实的，但是具体是因为哪些 SQL 是不清楚的。接着我们整理下得到以下的表格：<br />


| 事务1 | 事务2 |
| --- | --- |
| 某些 SQL | 某些 SQL |
| 某个 SQL 的 user_id = 5 行更新操作被阻塞了 | 某个 SQL 的 user_id = 4 获得了锁但是又阻塞了 |
| 某些 SQL | 某些 SQL |

可以得知，其实单从死锁日志分析是比较片面的，因为 user_id 为 4、5 这两个 update 操作是不会有互相阻塞的问题，肯定是有别的 SQL 影响，我们需要额外从业务日志分析才能还原完整的现场。<br />

<a name="i6azl"></a>
# 4. 从业务日志分析

<br />从死锁日志是不能完全知道导致的关键 SQL 和故障现场的整体流程，因此我们要借助业务日志来完成最后对故障现场的分析：<br />通过前面对业务日志的分析，我们知道最关键的调用方法是  `BizManagerImpl.transactionMoney` ，查看对应源码：
```java
@Override
@Transactional
public boolean transactionMoney(List<TransactionReqVO> transactionReqVOList) throws Exception {
    for (TransactionReqVO transactionReqVO : transactionReqVOList) {
        // 模拟业务操作
        Thread.sleep(1000);
        int updateCount = testTableService.update(transactionReqVO.getUserId(), transactionReqVO.getMoney());
        if (updateCount == 0) {
            log.error("转账异常：" + transactionReqVO);
        }
    }
    return true;
}
```
可以知道，应该是 for 循环事务的问题，但是具体是哪些 user_id 是不清楚的，接着我们查看业务日志的上下文，通过全链路 traceId（模拟） 做搜索，得到以下的日志：
```
[ConsumerThread2] org.example.controller.TestController    : 全局链路跟踪id:2的日志：[TransactionReqVO(userId=4, money=4), TransactionReqVO(userId=2, money=2), TransactionReqVO(userId=5, money=5)]
[ConsumerThread1] org.example.controller.TestController    : 全局链路跟踪id:1的日志：[TransactionReqVO(userId=5, money=5), TransactionReqVO(userId=1, money=1), TransactionReqVO(userId=4, money=4)]
```
分析到这一步，我们已经可以还原死锁场景了，事务流程图如下：

| 事务1 | 事务2 |
| --- | --- |
| UPDATE test_table SET money = money + 4 WHERE user_id = 4 | UPDATE test_table SET money = money + 5 WHERE user_id = 5 |
| UPDATE test_table SET money = money + 2 WHERE user_id = 2 | UPDATE test_table SET money = money + 1 WHERE user_id = 1 |
| UPDATE test_table SET money = money + 5 WHERE user_id = 5 | UPDATE test_table SET money = money + 4 WHERE user_id = 4 |



<a name="TWNt8"></a>
# 5. 业务日志、死锁日志结合分析

<br />将死锁日志分析得出的表格以及业务日志分析得出的表格，我们得出最终带有注释的事务表格：

| 事务1 | 事务2 |
| --- | --- |
| UPDATE test_table SET money = money + 4 WHERE user_id = 4 持有 user_id 为 4 的行锁 | UPDATE test_table SET money = money + 5 WHERE user_id = 5 持有 user_id 为 5 的行锁 |
| UPDATE test_table SET money = money + 2 WHERE user_id = 2 持有 user_id 为 2 的行锁 | UPDATE test_table SET money = money + 1 WHERE user_id = 1 持有 user_id 为 1 的行锁 |
| UPDATE test_table SET money = money + 5 WHERE user_id = 5 <br />阻塞中...等待持有 user_id 为 5 的行锁解锁 | UPDATE test_table SET money = money + 4 WHERE user_id = 4<br />阻塞中...user_id等待持有 user_id 为 4 的行锁解锁 |
|  | 回滚 |
| 提交 |  |


<br />可以知道，其实死锁日志的 SQL 是模糊的，需要从业务日志来复盘故障场景<br />

<a name="3pLYB"></a>
# 6. 善后

<br />模拟出了事务2的场景，我们就可以对回滚的 SQL 执行，来人工修复受到影响的用户数据。<br />也可以知道其实 `transactionMoney`  方法不应该加事务，因为该业务场景每个用户的更新是独立的不应该互相受到影响，但是当某条更新失败时，我们也要打印对应的日志。<br />这里我们就知道为什么之前大半年都没问题，最近才频发这种异常，因为只有当两个事务同时执行，并且两个事务中包含了相同的两个或两个以上的 user_id 才会可能触发该异常。而这种 user_id 都是所谓的大用户，像该示例中的 user_id 为 1、2 是小用户，虽然它们也受到了影响，但是频率是没有 user_id 为 4、5 这种大用户高的<br />

<a name="YRL0Y"></a>
# 7. 模拟项目源码

<br />为了模拟真实场景中的方法调用（消息接收调用执行），使用了线程来模拟。<br />并且使用线程睡眠来保证每个事务执行够长，来让每次模拟执行都必现异常。<br />![image.png](https://cdn.nlark.com/yuque/0/2020/png/203689/1594822263655-4dfc8c1c-49d8-4c00-b7d5-3dd9576a5168.png#align=left&display=inline&height=625&margin=%5Bobject%20Object%5D&name=image.png&originHeight=1250&originWidth=2622&size=261233&status=done&style=none&width=1311)<br />项目结构比较简单， Controller -> Manager -> Service -> Mapper -> DB，执行 `curl 'localhost:8080/test/consumer'` 后，查看命令行输出即可看到业务日志。对应的死锁日志需要到对应的数据库执行： `show engine innodb status` 后可看到。<br />

<a name="CE1Cq"></a>
# 8. 最后

<br />中间查阅了很多资料，发现有个项目总结了所有的死锁日志对应的可能 SQL：[https://github.com/aneasystone/mysql-deadlocks](https://github.com/aneasystone/mysql-deadlocks)，里面也讲解了加锁的各个细节过程。是非常值得一看的，以下为该项目的部分截图：<br />![image.png](https://cdn.nlark.com/yuque/0/2020/png/203689/1594822725837-6b99015b-69e7-43d3-ae32-3710062d79e5.png#align=left&display=inline&height=305&margin=%5Bobject%20Object%5D&name=image.png&originHeight=610&originWidth=1678&size=115947&status=done&style=none&width=839)<br />当遇到复杂的业务场景，尤其是不熟悉的时候，这个是一个很好的参考资料。

