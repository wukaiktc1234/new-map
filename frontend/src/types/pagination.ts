export interface IPage<T> {
  current: number;
  size: number;
  records: T[];
  total: number;
  pages: number;
  searchCount?: boolean;
  optimizeCountSql?: boolean;
  optimizeJoinOfCountSql?: boolean;
}
