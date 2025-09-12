/**
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */
package sanbing.jcpp.protocol.yunkuaichong.v150.cmd;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import sanbing.jcpp.infrastructure.util.codec.BCDUtil;
import sanbing.jcpp.infrastructure.util.jackson.JacksonUtil;
import sanbing.jcpp.proto.gen.UplinkProto.BmsAbortProto;
import sanbing.jcpp.proto.gen.UplinkProto.UplinkQueueMessage;
import sanbing.jcpp.protocol.ProtocolContext;
import sanbing.jcpp.protocol.annotation.ProtocolCmd;
import sanbing.jcpp.protocol.listener.tcp.TcpSession;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkCmdExe;
import sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongUplinkMessage;

import java.util.ArrayList;
import java.util.List;

import static sanbing.jcpp.protocol.yunkuaichong.YunKuaiChongProtocolConstants.ProtocolNames.*;

/**
 * 云快充1.5.0 充电阶段BMS中止
 */
@Slf4j
@ProtocolCmd(value = 0x1D, protocolNames = {V150, V160, V170})
public class YunKuaiChongV150BmsAbortULCmd extends YunKuaiChongUplinkCmdExe {

    @Override
    public void execute(TcpSession tcpSession, YunKuaiChongUplinkMessage yunKuaiChongUplinkMessage, ProtocolContext ctx) {
        log.debug("{} 云快充1.5.0充电阶段BMS中止", tcpSession);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(yunKuaiChongUplinkMessage.getMsgBody());

        ObjectNode additionalInfo = JacksonUtil.newObjectNode();
        // 1.交易流水号
        byte[] tradeNoBytes = new byte[16];
        byteBuf.readBytes(tradeNoBytes);
        String tradeNo = BCDUtil.toString(tradeNoBytes);

        // 2.桩编号
        byte[] pileCodeBytes = new byte[7];
        byteBuf.readBytes(pileCodeBytes);
        String pileCode = BCDUtil.toString(pileCodeBytes);

        // 3.枪号
        byte gunCodeByte = byteBuf.readByte();
        String gunCode = BCDUtil.toString(gunCodeByte);

        // 4.BMS中止充电原因
        byte reasonByte = byteBuf.readByte();
        additionalInfo.put("BMS中止充电原因", parseAbortReasons(reasonByte));

        // 5.BMS中止充电故障原因
        byte[] faultReasonBytes = new byte[2];
        additionalInfo.put("BMS中止充电故障原因", parseFaultReasons(faultReasonBytes));

        // 6.BMS中止充电错误原因
        byte errorReasonByte = byteBuf.readByte();
        additionalInfo.put("BMS中止充电错误原因", parseErrorReasons(errorReasonByte));

        BmsAbortProto proto = BmsAbortProto.newBuilder()
                .setPileCode(pileCode)
                .setGunCode(gunCode)
                .setTradeNo(tradeNo)
                .setAdditionalInfo(additionalInfo.toString())
                .build();

        // 转发到后端
        UplinkQueueMessage uplinkQueueMessage = uplinkMessageBuilder(pileCode, tcpSession, yunKuaiChongUplinkMessage)
                .setBmsAbortProto(proto)
                .build();

        tcpSession.getForwarder().sendMessage(uplinkQueueMessage);
    }

    /**
     * BMS中止充电原因枚举
     */
    @Getter
    public enum AbortReasonEnum {
        SOC_TARGET("需求SOC目标值") {
            @Override
            public String getStateDescription(int state) {
                return switch (state) {
                    case 0 -> "未达到所需SOC目标值";
                    case 1 -> "达到所需SOC目标值";
                    case 2 -> "不可信状态";
                    default -> "未知状态";
                };
            }
        },
        TOTAL_VOLTAGE("达到总电压设定值") {
            @Override
            public String getStateDescription(int state) {
                return switch (state) {
                    case 0 -> "未达到总电压设定值";
                    case 1 -> "达到总电压设定值";
                    case 2 -> "不可信状态";
                    default -> "未知状态";
                };
            }
        },
        CELL_VOLTAGE("达到单体电压设定值") {
            @Override
            public String getStateDescription(int state) {
                return switch (state) {
                    case 0 -> "未达到单体电压设定值";
                    case 1 -> "达到单体电压设定值";
                    case 2 -> "不可信状态";
                    default -> "未知状态";
                };
            }
        },
        CHARGER_INITIATED("充电机主动中止") {
            @Override
            public String getStateDescription(int state) {
                return switch (state) {
                    case 0 -> "充电机主动中止正常";
                    case 1 -> "充电机中止(收到CST帧)";
                    case 2 -> "不可信状态";
                    default -> "未知状态";
                };
            }
        };

        private final String description;

        AbortReasonEnum(String description) {
            this.description = description;
        }

        public abstract String getStateDescription(int state);
    }

    /**
     * 解析BMS中止充电原因字节
     *
     * @param reasonByte 1字节的原因字段值
     * @return 触发的原因描述列表
     */
    public String parseAbortReasons(byte reasonByte) {
        List<String> reasons = new ArrayList<>();

        // 将byte转为无符号整数 (0-255)
        int value = reasonByte & 0xFF;

        // 检查每组2bit的状态（从低位到高位）
        for (AbortReasonEnum reason : AbortReasonEnum.values()) {
            int bitPosition = reason.ordinal() * 2;
            int mask = 0b11 << bitPosition;  // 创建该组的位掩码
            int groupValue = (value & mask) >>> bitPosition;  // 提取组值
            reasons.add(reason.getStateDescription(groupValue));
        }
        return Joiner.on(", ").join(reasons);
    }

    /**
     * BMS中止充电故障原因枚举
     */
    @Getter
    public enum FaultReasonsEnum {
        INSULATION_FAULT("绝缘故障"){
            @Override
            public String getStateDescription(int state) {
                return switch (state) {
                    case 0 -> "绝缘正常";
                    case 1 -> "绝缘故障";
                    case 2 -> "不可信状态";
                    default -> "未知状态";
                };
            }
        },
        CONNECTOR_OVERHEAT("输出连接器过温故障"){
            @Override
            public String getStateDescription(int state) {
                return switch (state) {
                    case 0 -> "输出连接器正常";
                    case 1 -> "输出连接器过温故障";
                    case 2 -> "不可信状态";
                    default -> "未知状态";
                };
            }
        },
        BMS_COMPONENT_OVERHEAT("BMS元件过温故障") {
            @Override
            public String getStateDescription(int state) {
                return switch (state) {
                    case 0 -> "BMS元件正常";
                    case 1 -> "BMS元件过温故障";
                    case 2 -> "不可信状态";
                    default -> "未知状态";
                };
            }
        },
        CHARGING_CONNECTOR_FAULT("充电连接器故障") {
            @Override
            public String getStateDescription(int state) {
                return switch (state) {
                    case 0 -> "充电连接器正常";
                    case 1 -> "充电连接器故障";
                    case 2 -> "不可信状态";
                    default -> "未知状态";
                };
            }
        },
        BATTERY_OVERHEAT("电池组温度过高故障") {
            @Override
            public String getStateDescription(int state) {
                return switch (state) {
                    case 0 -> "电池组温度正常";
                    case 1 -> "电池组温度过高故障";
                    case 2 -> "不可信状态";
                    default -> "未知状态";
                };
            }
        },
        HIGH_VOLTAGE_RELAY_FAULT("高压继电器故障") {
            @Override
            public String getStateDescription(int state) {
                return switch (state) {
                    case 0 -> "高压继电器正常";
                    case 1 -> "高压继电器故障";
                    case 2 -> "不可信状态";
                    default -> "未知状态";
                };
            }
        },
        VOLTAGE_DETECTION_FAULT("检测点2电压检测故障") {
            @Override
            public String getStateDescription(int state) {
                return switch (state) {
                    case 0 -> "检测点2电压检测正常";
                    case 1 -> "检测点2电压检测故障";
                    case 2 -> "不可信状态";
                    default -> "未知状态";
                };
            }
        },
        OTHER_FAULT("其他故障") {
            @Override
            public String getStateDescription(int state) {
                return switch (state) {
                    case 0 -> "其他正常";
                    case 1 -> "其他故障";
                    case 2 -> "不可信状态";
                    default -> "未知状态";
                };
            }
        };

        private final String description;

        FaultReasonsEnum(String description) {
            this.description = description;
        }

        public abstract String getStateDescription(int state);

    }

    /**
     * 解析BMS中止充电故障原因
     * @param faultBytes 2字节的故障原因字段值
     * @return 触发的故障描述列表
     */
    public String parseFaultReasons(byte[] faultBytes) {
        List<String> faults = new ArrayList<>();

        if (faultBytes == null || faultBytes.length != 2) {
            throw new IllegalArgumentException("故障原因字段必须是2字节长度");
        }

        // 将2字节转换为无符号整数 (0-65535)
        int value = ((faultBytes[0] & 0xFF) << 8) | (faultBytes[1] & 0xFF);

        // 检查每组2bit的状态
        for (FaultReasonsEnum fault : FaultReasonsEnum.values()) {
            int bitPosition = fault.ordinal() * 2;
            int mask = 0b11 << bitPosition;  // 创建该组的位掩码
            int groupValue = (value & mask) >>> bitPosition;  // 提取组值
            faults.add(fault.getStateDescription(groupValue));
        }
        return Joiner.on(", ").join(faults);
    }

    /**
     * BMS中止充电错误原因枚举
     */
    @Getter
    public enum ErrorReasonsEnum {
        CURRENT_OVERFLOW("电流过大") {
            @Override
            public String getStateDescription(int state) {
                return switch (state) {
                    case 0 -> "电流正常";
                    case 1 -> "电流超过需求值";
                    case 2 -> "不可信状态";
                    default -> "未知状态";
                };
            }
        },
        VOLTAGE_ABNORMAL("电压异常") {
            @Override
            public String getStateDescription(int state) {
                return switch (state) {
                    case 0 -> "电压正常";
                    case 1 -> "电压异常";
                    case 2 -> "不可信状态";
                    default -> "未知状态";
                };
            }
        };

        private final String description;

        ErrorReasonsEnum(String description) {
            this.description = description;
        }

        public abstract String getStateDescription(int state);

    }

    /**
     * 解析BMS中止充电错误原因
     * @param errorByte 1字节的错误原因字段值
     * @return 触发的错误描述列表
     */
    public String parseErrorReasons(byte errorByte) {
        List<String> errors = new ArrayList<>();
        int value = errorByte & 0xFF;

        for (ErrorReasonsEnum error : ErrorReasonsEnum.values()) {
            int bitPosition = error.ordinal() * 2;
            int mask = 0b11 << bitPosition;
            int groupValue = (value & mask) >>> bitPosition;
            errors.add(error.getStateDescription(groupValue));
        }
        return Joiner.on(", ").join(errors);
    }

}
