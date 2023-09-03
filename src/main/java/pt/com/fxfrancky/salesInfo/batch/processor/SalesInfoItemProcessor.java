package pt.com.fxfrancky.salesInfo.batch.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import pt.com.fxfrancky.salesInfo.batch.dto.SalesInfoDTO;
import pt.com.fxfrancky.salesInfo.batch.mapper.SalesInfoMapper;
import pt.com.fxfrancky.salesInfo.domain.SalesInfo;

@Component
@RequiredArgsConstructor
@Slf4j
public class SalesInfoItemProcessor implements ItemProcessor<SalesInfoDTO, SalesInfo> {
    private final SalesInfoMapper salesInfoMapper;

    @Override
    public SalesInfo process(SalesInfoDTO item) throws Exception {
//        Thread.sleep(1000);
        log.info("processing the item: {}",item.toString());
        return salesInfoMapper.mapToEntity(item);
    }
}
