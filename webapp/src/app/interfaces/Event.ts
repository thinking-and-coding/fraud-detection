// import { RefObject } from "react";

// MSG
// id: "123456"
// event: "pay"
// accountUuid: "A"
// vtUuid: "B"
// metadata: null
// timestamp: 1737000000

export interface Event {
  id: string;
  event: string;
  accountUuid: string;
  vtUuid: string;
  metadata: Map<string, object>;
  timestamp: number;
}
