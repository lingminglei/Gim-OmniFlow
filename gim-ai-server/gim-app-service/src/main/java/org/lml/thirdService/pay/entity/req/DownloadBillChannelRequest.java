package org.lml.thirdService.pay.entity.req;

import lombok.*;

/**
 * 账单下载参数
 *
 * @author Hollis
 * @date 2025/07/01
 */

@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class DownloadBillChannelRequest  {

    /**
     * 账单token
     */
    private String token;
}
