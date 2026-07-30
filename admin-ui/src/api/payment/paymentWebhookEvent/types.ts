export type IsoDateString = string;
export type PaymentWebhookEventType =
  | 'PAYMENT_SUCCEEDED'
  | 'PAYMENT_FAILED'
  | 'PAYMENT_CANCELLED'
  | 'REFUND_SUCCEEDED'
  | 'CHARGEBACK_CREATED';
export type PaymentWebhookStatus = 'RECEIVED' | 'PROCESSED' | 'FAILED' | 'IGNORED';

export interface PaymentWebhookEventQuery extends PageQuery {
  providerEventId?: string;
  purchaseOrderNo?: string;
  sessionNo?: string;
  providerSessionNo?: string;
  eventType?: PaymentWebhookEventType | '';
  status?: PaymentWebhookStatus | '';
  providerCode?: string;
  beginTime?: IsoDateString;
  endTime?: IsoDateString;
}

export interface PaymentWebhookEventVO {
  id: string | number;
  providerCode: string;
  providerEventId: string;
  eventType: PaymentWebhookEventType;
  providerSessionNo: string;
  sessionNo: string;
  purchaseOrderNo: string;
  receivedTime: IsoDateString;
  status: PaymentWebhookStatus;
  failureReason: string;
  processingCount: number;
  lastProcessingTime: IsoDateString;
  createTime: IsoDateString;
  updateTime: IsoDateString;
}

export interface PaymentWebhookEventDetailVO extends PaymentWebhookEventVO {
  rawBody: string;
  signatureDigest: string;
}

export interface PaymentWebhookRetryResultVO {
  eventId: string | number;
  providerEventId: string;
  status: PaymentWebhookStatus;
  processingCount: number;
  lastProcessingTime: IsoDateString;
  failureReason: string;
}
