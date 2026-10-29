package org.javaboy.vhr.mapper;
import org.apache.ibatis.annotations.Param;
import org.javaboy.vhr.model.*;
import java.util.List;
public interface EcMapper {
    EcRule lockRule(Integer id);
    EcRecord lockRecord(Integer id);
    EcAudit receipt(String requestKey);
    int insertRule(EcRule rule);
    int updateRule(EcRule rule);
    int insertRecord(EcRecord record);
    int cancelRecord(EcRecord record);
    int insertAudit(EcAudit audit);
    List<EcRule> rules(@Param("q") EcQuery query);
    long ruleCount(@Param("q") EcQuery query);
    List<EcRecord> records(@Param("q") EcQuery query);
    long recordCount(@Param("q") EcQuery query);
}

