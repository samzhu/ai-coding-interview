"use client";

import { useMemo, useState } from "react";
import {
  ChevronRight,
  ChevronDown,
  Folder,
  FolderOpen,
  FileCode,
  FileText,
  File,
  Cog,
  Zap,
  RefreshCw,
} from "lucide-react";
import { useInterview } from "@/contexts/interview-context";
import { cn } from "@interview/shared/lib/utils";
import type { CheckpointFileState } from "@interview/shared/types";

interface FileExplorerProps {
  onCollapse: () => void;
  onRefresh?: () => void;
}

interface TreeNode {
  name: string;
  fullPath: string;
  isDir: boolean;
  children: TreeNode[];
}

function getWorkspacePrefix(
  files: Map<string, CheckpointFileState>
): string {
  const paths = Array.from(files.values()).map((f) => f.filePath);
  if (paths.length === 0) return "/workspace";
  const firstParts = paths[0].split("/").filter(Boolean);
  let prefix = "";
  for (let i = 0; i < firstParts.length - 1; i++) {
    const candidate = "/" + firstParts.slice(0, i + 1).join("/");
    if (paths.every((p) => p.startsWith(candidate + "/") || p === candidate)) {
      prefix = candidate;
    }
  }
  return prefix || "/workspace";
}

function buildTree(
  files: Map<string, CheckpointFileState>,
  workspacePrefix: string
): TreeNode[] {
  const dirMap: Record<string, unknown> = {};

  for (const { filePath } of files.values()) {
    const relative = filePath.startsWith(workspacePrefix + "/")
      ? filePath.slice(workspacePrefix.length + 1)
      : filePath.startsWith("/")
      ? filePath.slice(1)
      : filePath;

    const parts = relative.split("/").filter(Boolean);
    let cur = dirMap as Record<string, unknown>;
    let curPath = workspacePrefix;

    for (let i = 0; i < parts.length; i++) {
      const part = parts[i];
      curPath = curPath + "/" + part;
      if (i === parts.length - 1) {
        cur["\x00" + part] = filePath;
      } else {
        if (!cur[part]) cur[part] = { __path: curPath };
        cur = cur[part] as Record<string, unknown>;
      }
    }
  }

  function toNodes(obj: Record<string, unknown>): TreeNode[] {
    const nodes: TreeNode[] = [];
    for (const [key, val] of Object.entries(obj)) {
      if (key === "__path") continue;
      if (key.startsWith("\x00")) {
        nodes.push({
          name: key.slice(1),
          fullPath: val as string,
          isDir: false,
          children: [],
        });
      } else {
        const dirObj = val as Record<string, unknown>;
        nodes.push({
          name: key,
          fullPath: dirObj.__path as string,
          isDir: true,
          children: toNodes(dirObj),
        });
      }
    }
    return nodes.sort((a, b) => {
      if (a.isDir !== b.isDir) return a.isDir ? -1 : 1;
      return a.name.localeCompare(b.name);
    });
  }

  return toNodes(dirMap);
}

function collectAllDirPaths(nodes: TreeNode[]): Set<string> {
  const paths = new Set<string>();
  function walk(ns: TreeNode[]) {
    for (const n of ns) {
      if (n.isDir) {
        paths.add(n.fullPath);
        walk(n.children);
      }
    }
  }
  walk(nodes);
  return paths;
}

function FileIcon({ name, className }: { name: string; className?: string }) {
  const ext = name.slice(name.lastIndexOf(".")).toLowerCase();
  const iconClass = cn("shrink-0", className);
  if ([".java", ".kt"].includes(ext))
    return <FileCode size={13} className={cn(iconClass, "text-[#f0a500]")} />;
  if ([".py"].includes(ext))
    return <FileCode size={13} className={cn(iconClass, "text-[#3b7cb8]")} />;
  if ([".js", ".ts", ".mjs"].includes(ext))
    return <FileCode size={13} className={cn(iconClass, "text-[#cbcb41]")} />;
  if ([".md", ".txt"].includes(ext))
    return <FileText size={13} className={cn(iconClass, "text-[#72bfce]")} />;
  if ([".yml", ".yaml", ".gradle", ".toml", ".xml"].includes(ext))
    return <Cog size={13} className={cn(iconClass, "text-[#8ebbff]")} />;
  if (name === "gradlew" || name === "gradlew.bat")
    return <Zap size={13} className={cn(iconClass, "text-[#ce9178]")} />;
  return <File size={13} className={cn(iconClass, "text-[#858585]")} />;
}

interface TreeRowProps {
  node: TreeNode;
  depth: number;
  collapsed: Set<string>;
  onToggle: (path: string) => void;
  activeFilePath: string | null;
  onSelect: (path: string) => void;
}

function TreeRow({
  node,
  depth,
  collapsed,
  onToggle,
  activeFilePath,
  onSelect,
}: TreeRowProps) {
  const isExpanded = !collapsed.has(node.fullPath);
  const isActive = node.fullPath === activeFilePath;
  const indent = 8 + depth * 12;

  if (node.isDir) {
    return (
      <>
        <button
          onClick={() => onToggle(node.fullPath)}
          className="w-full flex items-center gap-1 py-[3px] text-xs transition-colors text-left hover:bg-[#2a2d2e] text-[#c8c8c8] group"
          style={{ paddingLeft: `${indent}px`, paddingRight: "6px" }}
        >
          {isExpanded ? (
            <ChevronDown
              size={11}
              className="shrink-0 text-[#858585] group-hover:text-[#aaa]"
            />
          ) : (
            <ChevronRight
              size={11}
              className="shrink-0 text-[#858585] group-hover:text-[#aaa]"
            />
          )}
          <span className="mx-0.5 shrink-0">
            {isExpanded ? (
              <FolderOpen size={13} className="text-[#e8c08b]" />
            ) : (
              <Folder size={13} className="text-[#e8c08b]" />
            )}
          </span>
          <span className="truncate font-medium text-[11px] tracking-tight">
            {node.name}
          </span>
        </button>
        {isExpanded &&
          node.children.map((child) => (
            <TreeRow
              key={child.fullPath}
              node={child}
              depth={depth + 1}
              collapsed={collapsed}
              onToggle={onToggle}
              activeFilePath={activeFilePath}
              onSelect={onSelect}
            />
          ))}
      </>
    );
  }

  return (
    <button
      onClick={() => onSelect(node.fullPath)}
      className={cn(
        "w-full flex items-center gap-1.5 py-[3px] text-[11px] transition-colors text-left border-l-2",
        isActive
          ? "bg-[#37373d] text-[#ffffff] border-l-[#007acc]"
          : "text-[#858585] hover:bg-[#2a2d2e] hover:text-[#cccccc] border-l-transparent"
      )}
      style={{ paddingLeft: `${indent + 3}px`, paddingRight: "6px" }}
      title={node.fullPath}
    >
      <FileIcon name={node.name} />
      <span className="truncate">{node.name}</span>
    </button>
  );
}

export function FileExplorer({ onCollapse, onRefresh }: FileExplorerProps) {
  const { state, openFile } = useInterview();
  const { files, activeFilePath } = state;

  const workspacePrefix = useMemo(() => getWorkspacePrefix(files), [files]);
  const tree = useMemo(
    () => buildTree(files, workspacePrefix),
    [files, workspacePrefix]
  );

  const allDirPaths = useMemo(() => collectAllDirPaths(tree), [tree]);
  const [collapsed, setCollapsed] = useState<Set<string>>(new Set());

  const effectiveCollapsed = useMemo(() => {
    const valid = new Set<string>();
    for (const p of collapsed) {
      if (allDirPaths.has(p)) valid.add(p);
    }
    return valid;
  }, [collapsed, allDirPaths]);

  function toggle(path: string) {
    setCollapsed((prev) => {
      const next = new Set(prev);
      if (next.has(path)) next.delete(path);
      else next.add(path);
      return next;
    });
  }

  const workspaceName = workspacePrefix.split("/").pop() ?? "workspace";

  return (
    <div className="flex flex-col h-full bg-[#252526] select-none">
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-2 shrink-0 border-b border-[#333]">
        <span className="text-[10px] font-semibold tracking-widest text-[#858585] uppercase">
          Explorer
        </span>
        <div className="flex items-center gap-1">
          {onRefresh && (
            <button
              onClick={onRefresh}
              className="text-[#858585] hover:text-[#cccccc] transition-colors p-0.5 rounded"
              title="重新載入檔案（終端機修改後使用）"
            >
              <RefreshCw size={11} />
            </button>
          )}
          <button
            onClick={onCollapse}
            className="text-[#858585] hover:text-[#cccccc] transition-colors text-sm leading-none px-1"
            title="折疊檔案瀏覽器"
          >
            «
          </button>
        </div>
      </div>

      {/* Workspace root label */}
      {files.size > 0 && (
        <div className="flex items-center gap-1.5 px-3 py-1.5 shrink-0">
          <span className="text-[10px] font-semibold tracking-wider text-[#bbb] uppercase truncate">
            {workspaceName}
          </span>
        </div>
      )}

      {/* Tree */}
      <div className="flex-1 overflow-y-auto overflow-x-hidden pb-2">
        {files.size === 0 ? (
          <p className="text-[11px] text-[#585858] px-3 py-2">載入中...</p>
        ) : tree.length === 0 ? (
          <p className="text-[11px] text-[#585858] px-3 py-2">無可顯示的檔案</p>
        ) : (
          tree.map((node) => (
            <TreeRow
              key={node.fullPath}
              node={node}
              depth={0}
              collapsed={effectiveCollapsed}
              onToggle={toggle}
              activeFilePath={activeFilePath}
              onSelect={openFile}
            />
          ))
        )}
      </div>
    </div>
  );
}
