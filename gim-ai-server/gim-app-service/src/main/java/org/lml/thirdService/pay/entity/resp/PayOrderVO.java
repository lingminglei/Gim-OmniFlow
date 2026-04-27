package org.lml.thirdService.pay.entity.resp;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.lml.thirdService.pay.constant.PayOrderState;

import java.io.Serializable;

/**
 * @author Hollis
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class PayOrderVO implements Serializable {

    private String payOrderId;

    private String payUrl;

    private PayOrderState orderState;

    private static final long serialVersionUID = 1L;
}
