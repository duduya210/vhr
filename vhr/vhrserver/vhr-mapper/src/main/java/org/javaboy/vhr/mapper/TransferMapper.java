package org.javaboy.vhr.mapper;

import org.apache.ibatis.annotations.Param;
import org.javaboy.vhr.model.*;
import java.util.List;

public interface TransferMapper {
    TransferEmployee lockEmployee(Integer id);
    List<TransferEmployee> searchEmployees(@Param("keyword") String keyword);
    List<Department> departments();
    List<Position> positions();
    Department department(Integer id);
    Position position(Integer id);
    TransferRecord findByRequestId(String requestId);
    int updateAssignment(TransferRecord record);
    int insertTransfer(TransferRecord record);
    int insertLog(TransferRecord record);
    List<TransferRecord> transfers(@Param("q") TransferQuery query);
    long transferCount(@Param("q") TransferQuery query);
    List<TransferOperationLog> logs(@Param("q") TransferQuery query);
    long logCount(@Param("q") TransferQuery query);
}
