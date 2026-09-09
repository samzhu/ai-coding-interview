import { Annotation } from "@codemirror/state";

/**
 * 設計說明：用 CodeMirror Annotation 標記某個 transaction 是 AI proposal apply。
 * updateListener 看到帶這個 annotation 的 transaction 就跳過，不上報為 manual edit。
 *
 * 為什麼不用 transaction.userEvent：userEvent 是字串類型且 CodeMirror 內建慣例是
 * "input"/"delete" 等通用語意，用自訂 annotation 更明確且不會與內建語意衝突。
 *
 * 注意：@uiw/react-codemirror 的受控模式（value prop 變更）透過內部 dispatch 更新文件，
 * 這些 transaction 不會帶 userEvent("input") 也不會帶此 annotation。
 * updateListener 以 transaction.isUserEvent("input") || transaction.isUserEvent("delete")
 * 為主要過濾依據，此 annotation 為補充保障。
 */
export const aiApplyAnnotation = Annotation.define<boolean>();
