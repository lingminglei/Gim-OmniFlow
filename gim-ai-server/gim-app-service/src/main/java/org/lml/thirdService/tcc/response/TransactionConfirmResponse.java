package org.lml.thirdService.tcc.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.lml.thirdService.tcc.entity.TransConfirmSuccessType;

import java.io.Serializable;

/**
 * @author Hollis
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class TransactionConfirmResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean success;

    private String errorCode;

    private String errorMsg;

    private TransConfirmSuccessType transConfirmSuccessType;

    public TransactionConfirmResponse(Boolean success, TransConfirmSuccessType transConfirmSuccessType) {
        this.success = success;
        this.transConfirmSuccessType = transConfirmSuccessType;
    }

    public TransactionConfirmResponse(Boolean success, String errorCode, String errorMsg) {
        this.success = success;
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }
}
