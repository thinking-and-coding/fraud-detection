import { Event } from "./Event";
import { RefObject } from "react";
import { StrategyPayload } from "./Strategy";

export interface Alert {
  alertId: string;
  strategyId: number;
  violatedStrategy: StrategyPayload;
  triggeringValue: number;
  triggeringEvent: Event;
  ref: RefObject<HTMLDivElement>;
}
