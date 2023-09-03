package pt.com.fxfrancky.salesInfo.batch.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import pt.com.fxfrancky.salesInfo.batch.dto.SalesInfoDTO;
import pt.com.fxfrancky.salesInfo.domain.SalesInfo;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface SalesInfoMapper {

    SalesInfo mapToEntity(SalesInfoDTO salesInfoDTO);
}
