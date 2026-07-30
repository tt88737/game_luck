export type IsoDateString = string;
export type PaymentSessionStatus = 'CREATED' | 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED' | 'EXPIRED';

export interface PaymentSessionQuery extends PageQuery {
  sessionNo?: string;
  purchaseOrderNo?: string;
  providerSessionNo?: string;
  memberId?: string | number;
  memberNo?: string;
  providerCode?: string;
  status?: PaymentSessionStatus | '';
  payCurrencyCode?: string;
  beginTime?: IsoDateString;
  endTime?: IsoDateString;
}

export interface PaymentSessionVO {
  id: string | number;
  sessionNo: string;
  purchaseOrderId: string | number;
  purchaseOrderNo: string;
  memberId: string | number;
  memberNo: string;
  providerCode: string;
  providerSessionNo: string;
  payCurrencyCode: string;
  payAmount: string;
  checkoutUrl: string;
  status: PaymentSessionStatus;
  expireTime: IsoDateString;
  completedTime: IsoDateString;
  createTime: IsoDateString;
  updateTime: IsoDateString;
}
