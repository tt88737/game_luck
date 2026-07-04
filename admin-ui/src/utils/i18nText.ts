import i18n from '@/lang';

const enTextMap: Record<string, string> = {
  搜索: 'Search',
  重置: 'Reset',
  新增: 'Add',
  删除: 'Delete',
  确定: 'Confirm',
  取消: 'Cancel',
  操作: 'Operation',
  状态: 'Status',
  币种: 'Currency',
  金额: 'Amount',
  备注: 'Remark',
  创建时间: 'Created At',
  更新时间: 'Updated At',
  取消时间: 'Cancelled At',
  会员ID: 'Member ID',
  订单号: 'Order No.',
  交易号: 'Transaction No.',
  业务单号: 'Business No.',
  来源: 'Source',
  来源类型: 'Source Type',
  失败原因: 'Failure Reason',
  查看详情: 'View Detail',
  正常: 'Normal',
  冻结: 'Frozen',
  禁用: 'Disabled',
  启用: 'Enabled',
  停用: 'Disabled',
  成功: 'Success',
  失败: 'Failed',
  已取消: 'Cancelled',
  待支付: 'Pending Payment',
  待下注: 'Pending Bet',
  已扣款: 'Debited',
  扣款失败: 'Debit Failed',
  已结算: 'Settled',
  结算失败: 'Settlement Failed',
  已释放: 'Released',
  待释放: 'Pending Release',
  部分释放: 'Partially Released',
  已消费: 'Consumed',
  无需释放: 'No Release Required',
  允许: 'Allow',
  禁止: 'Deny',
  具备: 'Available',
  不具备: 'Unavailable',
  需要: 'Required',
  不需要: 'Not Required',
  入账: 'Credit',
  扣账: 'Debit',
  提现: 'Withdrawal',
  兑换: 'Redemption',
  排序: 'Sort',
  精度: 'Precision',
  释放号: 'Release No.',
  冻结号: 'Freeze No.',
  注单号: 'Bet Order No.',
  局号: 'Round No.',
  游戏: 'Game',
  游戏编码: 'Game Code',
  下注金额: 'Bet Amount',
  派彩金额: 'Payout Amount',
  净额: 'Net Amount',
  下注交易号: 'Bet Transaction No.',
  结算交易号: 'Settlement Transaction No.',
  退款交易号: 'Refund Transaction No.',
  下注幂等键: 'Bet Idempotency Key',
  结算幂等键: 'Settlement Idempotency Key',
  退款幂等键: 'Refund Idempotency Key',
  支付方式: 'Payment Method',
  支付时间: 'Paid At',
  钱包交易号: 'Wallet Transaction No.',
  支付幂等键: 'Payment Idempotency Key',
  账户状态: 'Account Status',
  可用余额: 'Available Balance',
  冻结余额: 'Frozen Balance',
  冻结金额: 'Frozen Amount',
  变更前: 'Before',
  变更后: 'After',
  币种编码: 'Currency Code',
  币种名称: 'Currency Name',
  启用状态: 'Enabled Status',
  允许入账: 'Allow Credit',
  允许扣账: 'Allow Debit',
  允许冻结: 'Allow Freeze',
  提现能力: 'Withdrawal Capability',
  兑换能力: 'Redemption Capability',
  编辑能力: 'Edit Capabilities',
  编辑规则: 'Edit Rule',
  规则名称: 'Rule Name',
  释放模式: 'Release Mode',
  需要流水: 'Turnover Required',
  默认流水: 'Default Turnover',
  需要业务流水: 'Business Turnover Required',
  入账金额: 'Credit Amount',
  所需流水: 'Required Turnover',
  完成流水: 'Completed Turnover',
  模式: 'Mode',
  释放状态: 'Release Status',
  交易状态: 'Transaction Status',
  操作类型: 'Operation Type',
  冻结状态: 'Freeze Status',
  立即释放: 'Immediate Release',
  满足流水后释放: 'Release After Turnover',
  永不释放: 'Never Release',
  人工审核: 'Manual Review',
  模拟支付成功: 'Simulate Payment Success',
  取消订单: 'Cancel Order',
  取消退款: 'Cancel And Refund',
  模拟下注扣款: 'Simulate Bet Debit',
  模拟结算派彩: 'Simulate Settlement Payout',
  充值订单详情: 'Deposit Order Detail',
  模拟下注订单详情: 'Simulated Bet Order Detail',
  新增模拟充值订单: 'Add Simulated Deposit Order',
  新增模拟下注订单: 'Add Simulated Bet Order',
  编辑币种能力: 'Edit Currency Capabilities',
  新增钱包规则: 'Add Wallet Rule',
  编辑钱包规则: 'Edit Wallet Rule',
  '为空则自动生成': 'Auto generated when empty',
  '默认 SIMULATED': 'Default SIMULATED',
  '例如 GAME_PROFIT / DEPOSIT / PROMOTION': 'e.g. GAME_PROFIT / DEPOSIT / PROMOTION',
  新增成功: 'Added successfully',
  操作成功: 'Operation successful',
  取消成功: 'Cancelled successfully',
  下注扣款完成: 'Bet debit completed',
  结算派彩完成: 'Settlement payout completed',
  取消退款完成: 'Cancellation refund completed',
  '确认将该充值订单标记为模拟支付成功并执行钱包入账？': 'Confirm marking this deposit order as simulated payment success and crediting the wallet?',
  '确认取消该充值订单？': 'Confirm cancelling this deposit order?',
  '确认对该模拟下注订单执行钱包扣款？': 'Confirm debiting the wallet for this simulated bet order?',
  '确认对该模拟下注订单执行结算派彩？': 'Confirm settling payout for this simulated bet order?',
  '确认取消该模拟下注订单并退回下注金额？': 'Confirm cancelling this simulated bet order and refunding the bet amount?'
};

export const tt = (text?: string) => {
  if (!text) {
    return '';
  }
  const locale = i18n.global.locale.value;
  if (locale !== 'en_US') {
    return text;
  }
  if (enTextMap[text]) {
    return enTextMap[text];
  }
  if (text.startsWith('请输入')) {
    return `Enter ${tt(text.slice(3)).toLowerCase()}`;
  }
  if (text.startsWith('请选择')) {
    return `Select ${tt(text.slice(3)).toLowerCase()}`;
  }
  if (text.endsWith('不能为空')) {
    return `${tt(text.slice(0, -4))} is required`;
  }
  if (text.startsWith('默认 ')) {
    return `Default ${text.slice(3)}`;
  }
  if (text.startsWith('例如 ')) {
    return `e.g. ${text.slice(3)}`;
  }
  if (text.startsWith('确认') && text.endsWith('？')) {
    return `Confirm ${tt(text.slice(2, -1)).toLowerCase()}?`;
  }
  if (text.endsWith('成功')) {
    return `${tt(text.slice(0, -2))} successful`;
  }
  return text;
};
