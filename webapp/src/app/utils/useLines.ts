import { Alert, Strategy } from "app/interfaces";
import LeaderLine from "leader-line";
import { flattenDeep } from "lodash/fp";
import { RefObject, useCallback, useEffect, useState } from "react";

export const useLines: UseLines = (eventsRef, strategies, alerts) => {
  const [lines, setLines] = useState<Line[]>([]);

  const updateLines = useCallback(() => {
    lines.forEach(line => {
      try {
        line.line.position();
      } catch {
        // nothing
      }
    });
  }, [lines]);

  useEffect(() => {
    const newLines = flattenDeep<Line>(
      strategies.map(strategy => {
        const hasAlert = alerts.some(alert => alert.strategyId === strategy.id);

        const inputLine = new LeaderLine(eventsRef.current, strategy.ref.current, {
          color: hasAlert ? "#dc3545" : undefined,
          dash: { animation: true },
          endSocket: "left",
          startSocket: "right",
        }) as Line;

        const outputLines = alerts.reduce<Line[]>((acc, alert) => {
          if (alert.strategyId === strategy.id) {
            return [
              ...acc,
              new LeaderLine(strategy.ref.current, alert.ref.current, {
                color: "#fff",
                endPlugOutline: true,
                endSocket: "left",
                outline: true,
                outlineColor: "#dc3545",
                startSocket: "right",
              }) as Line,
            ];
          }
          return acc;
        }, []);

        return [inputLine, ...outputLines];
      })
    );

    setLines(newLines);

    return () => {
      newLines.forEach(line => line.line.remove());
    };
  }, [eventsRef, strategies, alerts]);

  return { lines, handleScroll: updateLines };
};

type UseLines = (
    eventsRef: RefObject<HTMLDivElement>,
    strategies: Strategy[],
    alerts: Alert[]
) => {
  lines: Line[];
  handleScroll: () => void;
};

export interface Line {
  line: {
    color: string;
    position: () => void;
    remove: () => void;
  };
  strategyId: number;
}
