import { RefObject } from "react";

export interface Strategy {
  id: number;
  strategyPayload: string;
  ref: RefObject<HTMLDivElement>;
}

export interface StrategyPayload {
  aggregateFieldName: string;
  aggregatorFunctionType: string;
  groupingKeyNames: string[];
  limit: number;
  limitOperatorType: string;
  windowMinutes: number;
  strategyState: string;
  events: string[];
}
