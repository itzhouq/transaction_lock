package org.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TestTableMapper {

    @Update("UPDATE test_table SET money = money + #{money} WHERE user_id = #{userId}")
    int update(@Param("userId") long userId, @Param("money") long money);

    @Update("UPDATE test_table SET money = money + #{money}")
    int updateMoney(@Param("money") long money);
}
