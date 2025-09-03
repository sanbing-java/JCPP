/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.app.adapter.controller;

import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sanbing.jcpp.app.service.PileProtocolService;
import sanbing.jcpp.proto.gen.DownlinkProto.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author 九筒
 */
@RestController
@RequestMapping("/test")
public class TestController extends BaseController {

    @Resource
    private PileProtocolService pileProtocolService;

    @GetMapping("/startCharge")
    public ResponseEntity<String> startCharge() {

        String orderNo = "ORD" + RandomStringUtils.secure().nextNumeric(20);
        String logicalCardNo = RandomStringUtils.secure().nextNumeric(12);
        String physicalCardNo = RandomStringUtils.secure().nextNumeric(12);

        pileProtocolService.startCharge("20231212000010", "01", new BigDecimal("50"), orderNo,
                logicalCardNo, physicalCardNo, null);

        return ResponseEntity.ok("success");
    }

    @GetMapping("/parallelStartCharge")
    public ResponseEntity<String> parallelStartCharge() {

        String orderNo = "PAR" + RandomStringUtils.secure().nextNumeric(20);
        String logicalCardNo = RandomStringUtils.secure().nextNumeric(12);
        String physicalCardNo = RandomStringUtils.secure().nextNumeric(12);
        String parallelNo = RandomStringUtils.secure().nextNumeric(6);

        pileProtocolService.startCharge("20231212000010", "01", new BigDecimal("100"),
                orderNo, logicalCardNo, physicalCardNo, parallelNo);

        return ResponseEntity.ok("success");
    }

    @GetMapping("/stopCharge")
    public ResponseEntity<String> stopCharge() {

        pileProtocolService.stopCharge("20231212000010", "01");

        return ResponseEntity.ok("success");
    }

    @GetMapping("/setQrcode")
    public ResponseEntity<String> setQrcode() {
        QrcodeModelProto rcodeModelProto = QrcodeModelProto.newBuilder()
                .setGunName("1号枪二维码")
                .setCode("www.baidu.com/rcode=1")
                .build();
        SetQrcodeRequest setQrcodeRequest = SetQrcodeRequest.newBuilder()
                .setPileCode("20231212000010")
                .setQrcodeModel(rcodeModelProto)
                .build();

        pileProtocolService.setQrcode(setQrcodeRequest);

        return ResponseEntity.ok("success");
    }
    @GetMapping("/restartPile")
    public ResponseEntity<String> restartPile() {

        pileProtocolService.restartPile("20231212000010", 1);

        return ResponseEntity.ok("success");
    }

    @GetMapping("/setPricing")
    public ResponseEntity<String> setPricing() {

        String pileCode = "20231212000010";

        FlagPriceProto flagPriceTop = FlagPriceProto.newBuilder()
                .setFlag(PricingModelFlag.TOP)
                .setElec("1.5")
                .setServ("0.5")
                .build();

        FlagPriceProto flagPricePeak = FlagPriceProto.newBuilder()
                .setFlag(PricingModelFlag.PEAK)
                .setElec("1.2")
                .setServ("0.4")
                .build();

        FlagPriceProto flagPriceFlat = FlagPriceProto.newBuilder()
                .setFlag(PricingModelFlag.FLAT)
                .setElec("1.0")
                .setServ("0.3")
                .build();

        FlagPriceProto flagPriceValley = FlagPriceProto.newBuilder()
                .setFlag(PricingModelFlag.VALLEY)
                .setElec("0.7")
                .setServ("0.2")
                .build();

        // 构建 PeriodProto 对象
        PeriodProto topPeriod1 = PeriodProto.newBuilder()
                .setSn(1)
                .setBegin("10:00")
                .setEnd("15:00")
                .setFlag(PricingModelFlag.TOP)
                .build();

        PeriodProto topPeriod2 = PeriodProto.newBuilder()
                .setSn(2)
                .setBegin("18:00")
                .setEnd("21:00")
                .setFlag(PricingModelFlag.TOP)
                .build();

        PeriodProto peakPeriod1 = PeriodProto.newBuilder()
                .setSn(3)
                .setBegin("07:00")
                .setEnd("10:00")
                .setFlag(PricingModelFlag.PEAK)
                .build();

        PeriodProto peakPeriod2 = PeriodProto.newBuilder()
                .setSn(4)
                .setBegin("15:00")
                .setEnd("18:00")
                .setFlag(PricingModelFlag.PEAK)
                .build();

        PeriodProto flatPeriod1 = PeriodProto.newBuilder()
                .setSn(5)
                .setBegin("06:00")
                .setEnd("07:00")
                .setFlag(PricingModelFlag.FLAT)
                .build();

        PeriodProto flatPeriod2 = PeriodProto.newBuilder()
                .setSn(6)
                .setBegin("21:00")
                .setEnd("23:00")
                .setFlag(PricingModelFlag.FLAT)
                .build();

        PeriodProto valleyPeriod = PeriodProto.newBuilder()
                .setSn(7)
                .setBegin("23:00")
                .setEnd("06:00")
                .setFlag(PricingModelFlag.VALLEY)
                .build();

        // 构建 flagPrice 映射
        HashMap<Integer, FlagPriceProto> flagPriceMap = new HashMap<>();
        flagPriceMap.put(PricingModelFlag.TOP_VALUE, flagPriceTop);
        flagPriceMap.put(PricingModelFlag.PEAK_VALUE, flagPricePeak);
        flagPriceMap.put(PricingModelFlag.FLAT_VALUE, flagPriceFlat);
        flagPriceMap.put(PricingModelFlag.VALLEY_VALUE, flagPriceValley);

        // 构建峰谷计价配置
        PeakValleyPricingProto peakValleyPricing = PeakValleyPricingProto.newBuilder()
                .putAllFlagPrice(flagPriceMap) // 设置尖峰平谷对应的价格
                .addPeriod(topPeriod1) // 添加尖峰时段1
                .addPeriod(topPeriod2) // 添加尖峰时段2
                .addPeriod(peakPeriod1) // 添加峰时段1
                .addPeriod(peakPeriod2) // 添加峰时段2
                .addPeriod(flatPeriod1) // 添加平时段1
                .addPeriod(flatPeriod2) // 添加平时段2
                .addPeriod(valleyPeriod) // 添加谷时段
                .build();
        
        // 构建 PricingModelProto 对象
        PricingModelProto pricingModel = PricingModelProto.newBuilder()
                .setType(PricingModelType.CHARGE) // 设置为充电计费模型
                .setRule(PricingModelRule.PEAK_VALLEY_PRICING) // 使用峰谷计费规则
                .setPeakValleyPricing(peakValleyPricing) // 设置峰谷计价配置
                .build();

        pileProtocolService.setPricing(pileCode,
                SetPricingRequest.newBuilder()
                        .setPileCode(pileCode)
                        .setPricingId(1000L)
                        .setPricingModel(pricingModel)
                        .build());

        return ResponseEntity.ok("success");
    }

    @GetMapping("/timePeriodPricing")
    public ResponseEntity<String> testTimePeriodPricing() {
        String pileCode = "TEST001";

        // 创建时段计价列表
        List<TimePeriodItemProto> timePeriodItems = new ArrayList<>();
        
        // 深夜时段 (00:00-06:00)
        timePeriodItems.add(TimePeriodItemProto.newBuilder()
                .setPeriodNo(1)
                .setStartTime("00:00:00")
                .setEndTime("06:00:00")
                .setElecPrice("0.40")
                .setServPrice("0.20")
                .setDescription("深夜时段")
                .build());
        
        // 早高峰时段 (06:00-10:00)
        timePeriodItems.add(TimePeriodItemProto.newBuilder()
                .setPeriodNo(2)
                .setStartTime("06:00:00")
                .setEndTime("10:00:00")
                .setElecPrice("0.80")
                .setServPrice("0.50")
                .setDescription("早高峰时段")
                .build());
        
        // 日间平时段 (10:00-18:00)
        timePeriodItems.add(TimePeriodItemProto.newBuilder()
                .setPeriodNo(3)
                .setStartTime("10:00:00")
                .setEndTime("18:00:00")
                .setElecPrice("0.65")
                .setServPrice("0.35")
                .setDescription("日间平时段")
                .build());
        
        // 晚高峰时段 (18:00-22:00)
        timePeriodItems.add(TimePeriodItemProto.newBuilder()
                .setPeriodNo(4)
                .setStartTime("18:00:00")
                .setEndTime("22:00:00")
                .setElecPrice("0.90")
                .setServPrice("0.60")
                .setDescription("晚高峰时段")
                .build());
        
        // 夜间时段 (22:00-24:00)
        timePeriodItems.add(TimePeriodItemProto.newBuilder()
                .setPeriodNo(5)
                .setStartTime("22:00:00")
                .setEndTime("23:59:59")
                .setElecPrice("0.50")
                .setServPrice("0.25")
                .setDescription("夜间时段")
                .build());

        // 构建时段计价配置
        TimePeriodPricingProto timePeriodPricing = TimePeriodPricingProto.newBuilder()
                .addAllPeriods(timePeriodItems)
                .build();

        // 构建 PricingModelProto 对象
        PricingModelProto pricingModel = PricingModelProto.newBuilder()
                .setType(PricingModelType.CHARGE) // 设置为充电计费模型
                .setRule(PricingModelRule.TIME_PERIOD_PRICING) // 使用时段计价规则
                .setTimePeriodPricing(timePeriodPricing) // 设置时段计价配置
                .build();

        pileProtocolService.setPricing(pileCode,
                SetPricingRequest.newBuilder()
                        .setPileCode(pileCode)
                        .setPricingId(2000L)
                        .setPricingModel(pricingModel)
                        .build());

        return ResponseEntity.ok("Time period pricing test success");
    }

    @GetMapping("/otaRequest")
    public ResponseEntity<String> otaRequest() {

        pileProtocolService.otaRequest(OtaRequest.newBuilder()
                    .setAddress("127.0.0.1")
                    .setExecutionControl(1)
                    .setDownloadTimeout(1)
                    .setPassword("123123")
                    .setFilePath("/user/data")
                    .setPileCode("20231212000010")
                    .setPileModel(1)
                    .setPilePower(200)
                    .setPort(8080)
                    .setUsername("bawan")
                    .build());

        return ResponseEntity.ok("success");
    }

    @GetMapping("/offlineCardBalanceUpdateRequest")
    public ResponseEntity<String> offlineCardBalanceUpdateRequest() {

        pileProtocolService.offlineCardBalanceUpdateRequest(OfflineCardBalanceUpdateRequest.newBuilder()
                .setCardNo("1000000000123456")
                .setPileCode("20231212000010")
                .setGunCode("01")
                .setLimitYuan("1000")
                .build());

        return ResponseEntity.ok("success");
    }

    @GetMapping("/offlineCardSyncRequest")
    public ResponseEntity<String> offlineCardSyncRequest() {

        List<CardInfo> cardInfos = Lists.newArrayList(CardInfo.newBuilder().setCardNo("1000000000123456").setLogicCardNo("1000000000123456").build(),
                CardInfo.newBuilder().setCardNo("1000000000123457").setLogicCardNo("1000000000123457").build(),
                CardInfo.newBuilder().setCardNo("1000000000123458").setLogicCardNo("1000000000123458").build());

        pileProtocolService.offlineCardSyncRequest(OfflineCardSyncRequest.newBuilder()
                .setPileCode("20231212000010")
                .setTotal(cardInfos.size())
                .addAllCardInfo(cardInfos)
                .build());

        return ResponseEntity.ok("success");
    }

    @GetMapping("/timeSync")
    public ResponseEntity<String> timeSync() {
        pileProtocolService.timeSync("20231212000010", LocalDateTime.now());
        return ResponseEntity.ok("success");
    }

}