"use client";

import { useCallback, useState } from "react";
import {
  ReactFlow,
  addEdge,
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  type Connection,
  type Node,
  type Edge,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";

const initialNodes: Node[] = [
  {
    id: "1",
    type: "input",
    data: { label: "Start" },
    position: { x: 250, y: 50 },
    style: { background: "#1e40af", color: "#fff", border: "1px solid #3b82f6", borderRadius: 8 },
  },
  {
    id: "2",
    data: { label: "Agent: GPT-4o" },
    position: { x: 250, y: 180 },
    style: { background: "#1f2937", color: "#fff", border: "1px solid #374151", borderRadius: 8 },
  },
  {
    id: "3",
    type: "output",
    data: { label: "End" },
    position: { x: 250, y: 310 },
    style: { background: "#14532d", color: "#fff", border: "1px solid #16a34a", borderRadius: 8 },
  },
];

const initialEdges: Edge[] = [
  { id: "e1-2", source: "1", target: "2", animated: true },
  { id: "e2-3", source: "2", target: "3", animated: true },
];

export default function WorkflowCanvas() {
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);

  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge({ ...params, animated: true }, eds)),
    [setEdges]
  );

  const addAgentNode = useCallback(() => {
    const id = `node-${Date.now()}`;
    setNodes((nds) => [
      ...nds,
      {
        id,
        data: { label: "New Agent" },
        position: { x: Math.random() * 300 + 100, y: Math.random() * 200 + 100 },
        style: { background: "#1f2937", color: "#fff", border: "1px solid #374151", borderRadius: 8 },
      },
    ]);
  }, [setNodes]);

  return (
    <div className="flex-1 relative">
      <div className="absolute top-3 left-3 z-10 flex gap-2">
        <button
          onClick={addAgentNode}
          className="px-3 py-1.5 text-xs bg-gray-800 hover:bg-gray-700 border border-gray-700 rounded-lg transition-colors"
        >
          + Add Agent Node
        </button>
      </div>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        fitView
        style={{ background: "#030712" }}
      >
        <Background color="#1f2937" gap={20} />
        <Controls style={{ background: "#111827", border: "1px solid #374151" }} />
        <MiniMap style={{ background: "#111827" }} nodeColor="#3b82f6" />
      </ReactFlow>
    </div>
  );
}
