// import { RefObject } from "react";

// MSG
// beneficiaryId: 42694
// eventTime: 1565965071385
// payeeId: 20908
// paymentAmount: 13.54
// paymentType: "CRD"
// eventId: 5954524216210268000

export interface Event {
  beneficiaryId: number;
  event: string;
  eventTime: number;
  payeeId: number;
  paymentAmount: number;
  paymentType: string;
  eventId: number;
}
